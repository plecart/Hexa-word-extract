package com.hexa

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.hexa.core.geo.LatLng
import com.hexa.location.HeadingSource
import com.hexa.location.PositionSource
import com.hexa.location.SharedPositionSource
import com.hexa.location.firstFixPosition
import com.hexa.map.CompassHeadingSource
import com.hexa.map.CurrentTileTracker.currentTile
import com.hexa.map.FusedLocationSource
import com.hexa.map.H3Grid
import com.hexa.map.HexGrid
import com.hexa.map.MapConfig
import com.hexa.world.TileContent
import com.hexa.world.WorldGenerator
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
     * Source du **cap boussole** de l'appareil, consommée par l'**orientation de l'avatar à l'arrêt**
     * (#100, cf. [com.hexa.map.MapScreen]) après le retrait du pilotage boussole de la caméra (#96).
     * Émet le cap **brut** ([CompassHeadingSource], capteur de vecteur de rotation) ; le lissage est
     * appliqué en aval par l'opérateur de flux
     * [smoothedHeading][com.hexa.location.HeadingSmoother.smoothedHeading], laissant le contrat de la
     * source inchangé (le point d'application migrera dans le sélecteur hybride #102). Construite
     * paresseusement au premier accès. Un seul consommateur (l'avatar) → **pas de partage à chaud**,
     * à la différence de [sharedPositionSource].
     */
    val headingSource: HeadingSource by lazy {
        CompassHeadingSource(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
    }

    /**
     * Signal de **premier fix GPS** : `null` tant qu'aucune position filtrée n'est connue, puis la
     * dernière position. Observé par la **machine à états de démarrage** ([com.hexa.startup.startupStage])
     * pour ne quitter l'écran de chargement qu'une fois la position du joueur disponible — jamais sur
     * un centre arbitraire. Dérivé de la **même** position partagée que la caméra et la grille, **sans**
     * instancier la caméra de poursuite ; sa valeur courante amorce aussi le viewport de la carte
     * directement sur le joueur (cf. [com.hexa.map.MapScreen]). Tenu chaud tant qu'une vue l'observe.
     */
    val premierFix: StateFlow<LatLng?> by lazy {
        firstFixPosition(
            positions = sharedPositionSource.positions(),
            scope = appScope,
            started = SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS),
        )
    }

    /**
     * Intégration H3 **partagée** par toute l'app : la grille hexagonale et le générateur de monde
     * (via [com.hexa.world.TileCenterLocator]) s'appuient sur cette unique instance, pour n'avoir
     * qu'une seule façade native vivante. Sur Android, H3 se charge depuis les jniLibs via
     * `newSystemInstance` (cf. [H3Grid]).
     */
    private val h3Grid: H3Grid by lazy { H3Grid(h3 = H3Core.newSystemInstance()) }

    val sharedGrid: HexGrid get() = h3Grid

    /**
     * Générateur de monde **partagé** : recalcule à la volée le contenu d'une tuile (gisements et
     * vitesses) à partir de la **même** intégration H3 partagée ([sharedGrid] comme
     * [com.hexa.world.TileCenterLocator]). Une seule instance pour l'inspection de tuile (indexée par
     * `Long`, cf. [com.hexa.map.MapScreen]) et la récolte (indexée par `String`, cf. [tileContentOf]).
     */
    val sharedWorldGenerator: WorldGenerator by lazy { WorldGenerator(centerLocator = sharedGrid) }

    /**
     * Résout l'index H3 **textuel** d'une tuile (cellule d'un bâtiment lu depuis Firestore) en son
     * centre, pour poser les modèles 3D des bâtiments sur la carte (cf.
     * [com.hexa.map.buildingPlacements]). Adossé à la **même** intégration H3 partagée que [sharedGrid].
     */
    val centerOfCell: (String) -> LatLng get() = h3Grid::centerOf

    /**
     * Recalcule le contenu (gisements et vitesses) de la tuile d'un bâtiment depuis son index H3
     * **textuel** — la source des vitesses du calcul de récolte ([com.hexa.player.HarvestCalculator]).
     * Combine la conversion `String → Long` ([H3Grid.toH3Index]) et le [sharedWorldGenerator] : aucune
     * donnée par tuile n'est stockée, tout est régénéré déterministiquement.
     */
    val tileContentOf: (String) -> TileContent
        get() = { cell -> sharedWorldGenerator.contentOf(h3Grid.toH3Index(cell)) }

    /**
     * Tuile courante **partagée** : la cellule H3 sous le joueur, lissée par hystérésis
     * ([CurrentTileTracker]) à partir de la position filtrée. `null` tant qu'aucune position n'est
     * connue. La même valeur sert la grille (état « courante ») et l'inspection de tuile (badge « vous
     * êtes ici »), pour un suivi unique et cohérent. Tenue chaude tant qu'une vue l'observe.
     */
    val sharedCurrentTile: StateFlow<Long?> by lazy {
        sharedPositionSource.positions()
            .currentTile(sharedGrid, MapConfig.TILE_HYSTERESIS_MARGIN_M)
            .stateIn(appScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), null)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
    }
}
