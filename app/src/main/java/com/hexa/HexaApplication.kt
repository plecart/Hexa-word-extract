package com.hexa

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.hexa.location.PositionSource
import com.hexa.location.SharedPositionSource
import com.hexa.map.CurrentTileTracker.currentTile
import com.hexa.map.FusedLocationSource
import com.hexa.map.H3Grid
import com.hexa.map.HexGrid
import com.hexa.map.MapConfig
import com.uber.h3core.H3Core
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Point d'entrée de l'application. Active le **cache offline persistant** de Firestore avant toute
 * lecture/écriture : l'app démarre et lit le document joueur sans réseau après un premier lancement
 * réussi (cf. PRD #5, user story 14). À configurer ici, une seule fois, avant le premier accès au
 * SDK.
 *
 * Sert aussi de **racine de composition** pour les dépendances à l'échelle du processus, partagées
 * une fois pour toutes : la position GPS filtrée ([sharedPositionSource]), l'intégration H3
 * ([sharedGrid]) et la tuile courante qui en découle ([sharedCurrentTile]).
 */
class HexaApplication : Application() {
    /**
     * Portée du processus hébergeant les flux chauds partagés entre ViewModels (position GPS).
     *
     * Sur **[Dispatchers.Main]** : les consommateurs (ViewModels) collectent depuis `viewModelScope`
     * (Main), et la source GPS livre ses callbacks sur le looper principal. Garder le partage sur Main
     * évite un saut de thread inutile — et, surtout, le démarrage de `shareIn(WhileSubscribed)` ne se
     * propageait pas de façon fiable depuis un dispatcher de fond ici (l'amont GPS n'était jamais
     * souscrit).
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Position GPS filtrée **partagée** par toutes les vues (caméra de poursuite, grille hexagonale) :
     * un seul abonnement GPS pour l'app, et des filtres qui ne divergent pas. Construite
     * paresseusement au premier accès — donc après que [com.hexa.map.LocationPermissionGate] a accordé
     * la permission — et maintenue tant qu'au moins une vue l'observe.
     */
    val sharedPositionSource: PositionSource by lazy {
        SharedPositionSource(
            delegate = FusedLocationSource(
                client = LocationServices.getFusedLocationProviderClient(this),
                intervalMs = MapConfig.GPS_INTERVAL_MS,
                smoothingFactor = MapConfig.POSITION_SMOOTHING_FACTOR,
                accuracyThresholdM = MapConfig.ACCURACY_THRESHOLD_M,
            ),
            scope = appScope,
        )
    }

    /**
     * Intégration H3 **partagée** par toute l'app : la grille hexagonale et le générateur de monde
     * (via [com.hexa.world.TileCenterLocator]) s'appuient sur cette unique instance, pour n'avoir
     * qu'une seule façade native vivante. Sur Android, H3 se charge depuis les jniLibs via
     * `newSystemInstance` (cf. [H3Grid]).
     */
    val sharedGrid: HexGrid by lazy { H3Grid(h3 = H3Core.newSystemInstance()) }

    /**
     * Tuile courante **partagée** : la cellule H3 sous le joueur, lissée par hystérésis
     * ([CurrentTileTracker]) à partir de la position filtrée. `null` tant qu'aucune position n'est
     * connue. La même valeur sert la grille (état « courante ») et l'inspection de tuile (badge « vous
     * êtes ici »), pour un suivi unique et cohérent. Tenue chaude tant qu'une vue l'observe.
     */
    val sharedCurrentTile: StateFlow<Long?> by lazy {
        sharedPositionSource.positions()
            .currentTile(sharedGrid, MapConfig.TILE_HYSTERESIS_MARGIN_M)
            .stateIn(appScope, SharingStarted.WhileSubscribed(SHARE_STOP_MS), null)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
    }

    private companion object {
        /** Délai de maintien des flux partagés après la dernière désinscription (rotation d'écran). */
        const val SHARE_STOP_MS = 5_000L
    }
}
