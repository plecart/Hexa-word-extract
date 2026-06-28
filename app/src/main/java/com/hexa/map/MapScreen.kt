package com.hexa.map

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.HexaApplication
import com.hexa.core.geo.LatLng
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.PositionSource
import com.hexa.player.PlacedBuilding
import com.hexa.world.TileContent
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.AnnotatedFeature
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Durée d'interpolation de la caméra vers chaque nouvelle pose. Proche de la cadence d'échantillonnage
 * du cap (~100 ms) : assez courte pour que chaque animation aboutisse avant la suivante (sinon
 * l'ease-in-out, sans cesse relancé, figerait la caméra) et donne un suivi fluide.
 */
private const val FOLLOW_EASE_MS = 200L

/**
 * Écran carte : la **caméra de poursuite** à la troisième personne.
 *
 * La permission de localisation **et** le premier fix GPS sont garantis **en amont** par la machine à
 * états de démarrage ([com.hexa.startup.startupStage], cf. [com.hexa.MainActivity]) : cet écran n'est
 * composé qu'une fois la position du joueur connue, ce qui permet d'amorcer le viewport directement
 * sur lui (jamais de centre arbitraire visible).
 *
 * @param placedBuildings flux des bâtiments posés, rendus en **modèles 3D** sur la carte (cf.
 *   [com.hexa.player.PlayerViewModel.placedBuildings]).
 * @param extractorStock flux du nombre d'extracteurs prêts à poser, qui conditionne l'apparition du
 *   marqueur « + » sur la tuile courante (cf. [com.hexa.player.PlayerViewModel.extractorStock]).
 * @param onPlaceExtracteur pose un extracteur sur la tuile dont l'index H3 est fourni.
 */
@Composable
fun MapScreen(
    placedBuildings: Flow<List<PlacedBuilding>>,
    extractorStock: StateFlow<Int>,
    onPlaceExtracteur: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChaseCameraMap(
        placedBuildings = placedBuildings,
        extractorStock = extractorStock,
        onPlaceExtracteur = onPlaceExtracteur,
        modifier = modifier,
    )
}

/**
 * Carte Mapbox plein écran avec **caméra de poursuite à la troisième personne**.
 *
 * La caméra suit la position GPS filtrée fournie par [ChaseCameraViewModel], inclinée et orientée
 * selon le cap lissé de la boussole. Elle est **verrouillée en permanence sur le joueur** : pan,
 * rotation et inclinaison manuels sont désactivés (aucun geste ne décentre le joueur) ; seul le zoom
 * au pincement reste actif et borné à [MapConfig.MIN_ZOOM]–[MapConfig.MAX_ZOOM].
 *
 * Le token public est fourni au SDK en amont (cf. [com.hexa.MainActivity]).
 */
@Composable
private fun ChaseCameraMap(
    placedBuildings: Flow<List<PlacedBuilding>>,
    extractorStock: StateFlow<Int>,
    onPlaceExtracteur: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HexaApplication
    val viewModel: ChaseCameraViewModel =
        viewModel(factory = chaseCameraViewModelFactory(context, app.sharedPositionSource))
    val gridViewModel: HexGridViewModel =
        viewModel(factory = hexGridViewModelFactory(app.sharedCurrentTile, app.sharedGrid))
    val inspectionViewModel: TileInspectionViewModel =
        viewModel(
            factory = tileInspectionViewModelFactory(
                app.sharedGrid,
                app.sharedWorldGenerator::contentOf,
                app.sharedCurrentTile,
            ),
        )

    val camera by viewModel.cameraState.collectAsStateWithLifecycle()
    val gridCells by gridViewModel.cells.collectAsStateWithLifecycle()
    val inspection by inspectionViewModel.inspection.collectAsStateWithLifecycle()

    // Bâtiments posés → placements de modèles 3D : la résolution cellule → centre (H3 natif) passe par
    // l'intégration partagée de l'application, la même que la grille et l'inspection.
    val buildings by placedBuildings.collectAsStateWithLifecycle(initialValue = emptyList())
    val placements = remember(buildings) { buildingPlacements(buildings, app.centerOfCell) }

    // Pose d'un extracteur : la tuile courante porte un marqueur « + » dès qu'elle est libre et qu'il
    // reste du stock. La décision est purement dérivée de l'état observé (cf. extractorPlacementCell) ;
    // un tap sur le marqueur ouvre la liste des bâtiments à poser.
    val currentTile by app.sharedCurrentTile.collectAsStateWithLifecycle()
    val stock by extractorStock.collectAsStateWithLifecycle()
    val placementCell =
        remember(currentTile, buildings, stock) {
            extractorPlacementCell(currentTile, buildings, stock, app.sharedGrid::toH3String)
        }
    var placementSheetOpen by rememberSaveable { mutableStateOf(false) }

    // Position de l'avatar : la **même** position GPS filtrée partagée que la caméra (une seule
    // trajectoire), suivie indépendamment de la pose de poursuite.
    val avatarPosition by remember { app.sharedPositionSource.positions() }
        .collectAsStateWithLifecycle(initialValue = null)

    // La grille suit le palier d'anneaux du zoom de poursuite courant (et du zoom au pincement, qui
    // se répercute sur la pose).
    LaunchedEffect(camera?.zoomLevel) {
        camera?.zoomLevel?.let(gridViewModel::onZoomChanged)
    }

    // Viewport amorcé **directement sur le joueur** : cet écran n'étant composé qu'une fois le premier
    // fix connu (cf. machine à états de démarrage), la position partagée est déjà disponible, et la
    // carte s'affiche centrée sur lui sans glisser depuis un centre de repli. [MapConfig.DEFAULT_CENTER]
    // ne sert que de garde défensive si la valeur manquait — jamais montré en pratique.
    val initialCenter = app.premierFix.value
        ?: LatLng(MapConfig.DEFAULT_CENTER_LAT, MapConfig.DEFAULT_CENTER_LON)
    val viewportState =
        rememberMapViewportState {
            setCameraOptions {
                center(Point.fromLngLat(initialCenter.lngDeg, initialCenter.latDeg))
                zoom(MapConfig.DEFAULT_ZOOM)
            }
        }

    // Caméra verrouillée sur le joueur : on coupe via les **réglages de gestes** tout ce qui la
    // décentrerait, la désorienterait ou la ferait « sauter » — pan, pan au pincement, rotation,
    // inclinaison, zooms double-tap / quick-zoom. Seul le pincement à deux doigts subsiste pour zoomer
    // (`pinchToZoomEnabled` laissé au défaut). Les réglages **doivent** passer par [MapState] : le
    // wrapper Compose les applique et les ré-applique en continu, alors qu'un `gestures.updateSettings`
    // ponctuel serait réinitialisé.
    val mapState =
        rememberMapState {
            gesturesSettings = GesturesSettings {
                scrollEnabled = false
                pinchScrollEnabled = false
                rotateEnabled = false
                pitchEnabled = false
                doubleTapToZoomInEnabled = false
                doubleTouchToZoomOutEnabled = false
                quickZoomEnabled = false
            }
        }

    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            mapState = mapState,
            style = { MapStyle(style = MapConfig.STYLE_URL) },
            // Ornements parasites masqués par lambda vide : la barre d'échelle n'apporte rien au jeu,
            // et la boussole est sans objet puisque la caméra suit le cap en permanence (tap-to-north
            // inutile). Logo et attribution Mapbox laissés au défaut → restent affichés (CGU Mapbox).
            scaleBar = {},
            compass = {},
            // Un tap ouvre l'inspection de la tuile touchée : Mapbox fournit la coordonnée carte du
            // point, le ViewModel en résout la cellule H3 et en recalcule le contenu à la volée. Le
            // clic passe par le paramètre du composable (et non le plugin gestures, que le wrapper
            // Compose réinitialise) ; `true` consomme l'événement.
            onMapClickListener = OnMapClickListener { point ->
                inspectionViewModel.inspectAt(LatLng(latDeg = point.latitude(), lngDeg = point.longitude()))
                true
            },
        ) {
            // À chaque nouvelle pose de poursuite, glisser la caméra vers elle ; tant que la première
            // pose n'est pas connue (pose nulle, avant le premier fix), ne rien imposer.
            MapEffect(camera) { mapView ->
                val pose = camera ?: return@MapEffect
                mapView.mapboxMap.easeTo(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(pose.center.lngDeg, pose.center.latDeg))
                        .zoom(pose.zoomLevel)
                        .pitch(pose.pitchDeg)
                        .bearing(pose.bearingDeg)
                        .build(),
                    MapAnimationOptions.Builder().duration(FOLLOW_EASE_MS).build(),
                )
            }
            // Redessine la grille hexagonale à chaque nouvel ensemble de cellules (changement de
            // tuile courante ou de palier de zoom) ; la source GeoJSON n'est créée qu'une fois.
            MapEffect(gridCells) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showHexGrid(gridCells) }
            }
            // Redessine les modèles 3D des bâtiments à chaque changement de la liste des placements
            // (apparition d'un bâtiment, changement de tuile) ; la source/couche n'est créée qu'une fois.
            MapEffect(placements) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showBuildingModels(placements) }
            }
            // Repose le modèle de l'avatar à chaque nouvelle position lissée ; la source/couche n'est
            // créée qu'une fois, ensuite seul le point est réinjecté (suivi fluide en marche).
            MapEffect(avatarPosition) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showAvatar(avatarPosition) }
            }
            // Marqueur « + » de pose, ancré au centre de la tuile courante tant qu'une pose est possible
            // ([placementCell] non nul). Un tap ouvre la liste des bâtiments ; la pose le fait disparaître
            // (tuile devenue occupée / stock épuisé), la décision étant recalculée à chaque émission.
            placementCell?.let { cell ->
                val center = app.centerOfCell(cell)
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature(Point.fromLngLat(center.lngDeg, center.latDeg)))
                        .allowOverlap(true)
                        .build(),
                ) {
                    ExtractorPlacementMarker(onClick = { placementSheetOpen = true })
                }
            }
            // Bornes dures de zoom : aucun pincement ne peut sortir de [MIN_ZOOM, MAX_ZOOM]. Le verrou
            // des gestes (pan, rotation…) vit, lui, dans `mapState.gesturesSettings` ci-dessus.
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.setBounds(
                    CameraBoundsOptions.Builder()
                        .minZoom(MapConfig.MIN_ZOOM)
                        .maxZoom(MapConfig.MAX_ZOOM)
                        .build(),
                )
                mapView.gestures.addOnScaleListener(
                    object : OnScaleListener {
                        override fun onScaleBegin(detector: StandardScaleGestureDetector) = Unit

                        override fun onScale(detector: StandardScaleGestureDetector) = Unit

                        // Le pincement ajuste le zoom sans décentrer ; on répercute la valeur (bornée
                        // par le contrôleur) dans la pose pour qu'elle persiste à la prochaine poursuite.
                        override fun onScaleEnd(detector: StandardScaleGestureDetector) =
                            viewModel.onUserZoom(mapView.mapboxMap.cameraState.zoom)
                    },
                )
            }
        }

        inspection?.let { open ->
            TileInspectionSheet(inspection = open, onDismiss = inspectionViewModel::dismiss)
        }

        // Liste des bâtiments à poser, ouverte par le marqueur « + ». Bornée à une pose possible
        // ([placementCell] non nul) : la pose referme le panneau et le marqueur s'efface de lui-même.
        if (placementSheetOpen) {
            placementCell?.let { cell ->
                BuildingPlacementSheet(
                    extractorStock = stock,
                    onPlaceExtracteur = {
                        onPlaceExtracteur(cell)
                        placementSheetOpen = false
                    },
                    onDismiss = { placementSheetOpen = false },
                )
            }
        }
    }
}

/**
 * Fabrique le [ChaseCameraViewModel] en câblant la position GPS filtrée **partagée**
 * ([positionSource], cf. [HexaApplication.sharedPositionSource]) et la boussole de l'appareil
 * ([CompassHeadingSource]) pour le cap, avec les réglages de [MapConfig]. La permission de
 * localisation est garantie en amont par [LocationPermissionGate].
 */
private fun chaseCameraViewModelFactory(context: Context, positionSource: PositionSource) = viewModelFactory {
    initializer {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        ChaseCameraViewModel(
            positionSource = positionSource,
            headingSource = CompassHeadingSource(sensorManager),
            config = ChaseCameraConfig(
                pitchDeg = MapConfig.PITCH,
                followZoom = MapConfig.FOLLOW_ZOOM,
                minZoom = MapConfig.MIN_ZOOM,
                maxZoom = MapConfig.MAX_ZOOM,
            ),
            headingSmoothingFactor = MapConfig.HEADING_SMOOTHING_FACTOR,
        )
    }
}

/**
 * Fabrique le [HexGridViewModel] en câblant la **tuile courante partagée** ([currentTile]) et
 * l'intégration H3 de production partagée ([grid]) — les mêmes que celles servies à l'inspection de
 * tuile, pour un suivi unique.
 */
private fun hexGridViewModelFactory(currentTile: StateFlow<Long?>, grid: HexGrid) = viewModelFactory {
    initializer {
        HexGridViewModel(currentTile = currentTile, grid = grid)
    }
}

/**
 * Fabrique le [TileInspectionViewModel] en câblant la **même** intégration H3 partagée ([grid]), le
 * **même** générateur de monde partagé ([contentOf], cf. [com.hexa.HexaApplication.sharedWorldGenerator])
 * et la **même** tuile courante partagée ([currentTile]) que la grille : la résolution tap → cellule,
 * le contenu de tuile et la récolte partagent une seule façade native et un seul générateur.
 */
private fun tileInspectionViewModelFactory(
    grid: HexGrid,
    contentOf: (Long) -> TileContent,
    currentTile: StateFlow<Long?>,
) = viewModelFactory {
    initializer {
        TileInspectionViewModel(grid = grid, contentOf = contentOf, currentTile = currentTile)
    }
}
