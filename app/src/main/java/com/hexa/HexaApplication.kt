package com.hexa

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.hexa.location.PositionSource
import com.hexa.location.SharedPositionSource
import com.hexa.map.FusedLocationSource
import com.hexa.map.MapConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Point d'entrée de l'application. Active le **cache offline persistant** de Firestore avant toute
 * lecture/écriture : l'app démarre et lit le document joueur sans réseau après un premier lancement
 * réussi (cf. PRD #5, user story 14). À configurer ici, une seule fois, avant le premier accès au
 * SDK.
 *
 * Sert aussi de **racine de composition** pour les dépendances à l'échelle du processus : la position
 * GPS filtrée y est partagée une fois pour toutes (cf. [sharedPositionSource]).
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

    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
    }
}
