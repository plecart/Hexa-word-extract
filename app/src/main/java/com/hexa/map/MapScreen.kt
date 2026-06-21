package com.hexa.map

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.HexaApplication
import com.hexa.R
import com.hexa.core.geo.LatLng
import com.hexa.location.CameraMode
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.PositionSource
import com.hexa.ui.theme.HexaActionButton
import com.hexa.world.WorldGenerator
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
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
 * Écran carte : derrière la **porte de permission de localisation**, affiche la caméra de poursuite.
 *
 * Tant que `ACCESS_FINE_LOCATION` n'est pas accordée, [LocationPermissionGate] présente la demande
 * puis, en cas de refus, un état explicite ; une fois accordée, [ChaseCameraMap] s'affiche.
 *
 * @param builtCells flux des index H3 des cellules bâties à surligner (la base posée, cf.
 *   [com.hexa.player.PlayerViewModel.builtCells]).
 */
@Composable
fun MapScreen(builtCells: Flow<Set<String>>, modifier: Modifier = Modifier) {
    LocationPermissionGate(modifier = modifier) {
        ChaseCameraMap(builtCells = builtCells, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Carte Mapbox plein écran avec **caméra de poursuite à la troisième personne**.
 *
 * La caméra suit la position GPS filtrée fournie par [ChaseCameraViewModel], inclinée et orientée
 * selon le cap lissé de la boussole. Un déplacement au doigt suspend la poursuite (mode libre) et
 * fait apparaître un bouton de recentrage ; le zoom au pincement reste actif et borné à
 * [MapConfig.MIN_ZOOM]–[MapConfig.MAX_ZOOM] pendant la poursuite.
 *
 * Le token public est fourni au SDK en amont (cf. [com.hexa.MainActivity]).
 */
@Composable
private fun ChaseCameraMap(builtCells: Flow<Set<String>>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HexaApplication
    val viewModel: ChaseCameraViewModel =
        viewModel(factory = chaseCameraViewModelFactory(context, app.sharedPositionSource))
    val gridViewModel: HexGridViewModel =
        viewModel(factory = hexGridViewModelFactory(app.sharedCurrentTile, app.sharedGrid, builtCells))
    val inspectionViewModel: TileInspectionViewModel =
        viewModel(factory = tileInspectionViewModelFactory(app.sharedGrid, app.sharedCurrentTile))

    val camera by viewModel.cameraState.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val gridCells by gridViewModel.cells.collectAsStateWithLifecycle()
    val inspection by inspectionViewModel.inspection.collectAsStateWithLifecycle()

    // La grille suit le palier d'anneaux du zoom de poursuite courant (et du zoom au pincement, qui
    // se répercute sur la pose). En mode libre, le dernier zoom de poursuite est conservé.
    LaunchedEffect(camera?.zoomLevel) {
        camera?.zoomLevel?.let(gridViewModel::onZoomChanged)
    }

    val viewportState =
        rememberMapViewportState {
            setCameraOptions {
                center(Point.fromLngLat(MapConfig.DEFAULT_CENTER_LON, MapConfig.DEFAULT_CENTER_LAT))
                zoom(MapConfig.DEFAULT_ZOOM)
            }
        }

    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
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
            // À chaque nouvelle pose de poursuite, glisser la caméra vers elle ; en mode libre
            // (pose nulle), ne rien imposer pour laisser la carte là où l'utilisateur l'a déplacée.
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
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.setBounds(
                    CameraBoundsOptions.Builder()
                        .minZoom(MapConfig.MIN_ZOOM)
                        .maxZoom(MapConfig.MAX_ZOOM)
                        .build(),
                )
                mapView.gestures.addOnMoveListener(
                    object : OnMoveListener {
                        // Un déplacement au doigt rend la main à l'utilisateur (mode libre).
                        override fun onMoveBegin(detector: MoveGestureDetector) = viewModel.onUserPan()

                        override fun onMove(detector: MoveGestureDetector): Boolean = false

                        override fun onMoveEnd(detector: MoveGestureDetector) = Unit
                    },
                )
                mapView.gestures.addOnScaleListener(
                    object : OnScaleListener {
                        override fun onScaleBegin(detector: StandardScaleGestureDetector) = Unit

                        override fun onScale(detector: StandardScaleGestureDetector) = Unit

                        // Le pincement ajuste le zoom sans quitter la poursuite ; on répercute la valeur.
                        override fun onScaleEnd(detector: StandardScaleGestureDetector) =
                            viewModel.onUserZoom(mapView.mapboxMap.cameraState.zoom)
                    },
                )
            }
        }

        inspection?.let { open ->
            TileInspectionSheet(inspection = open, onDismiss = inspectionViewModel::dismiss)
        }

        if (mode == CameraMode.FREE) {
            HexaActionButton(
                text = stringResource(R.string.recenter_camera),
                onClick = viewModel::recenter,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
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
 * tuile, pour un suivi unique. [builtCells] fournit les cellules à surligner comme « bâties » (la
 * base posée).
 */
private fun hexGridViewModelFactory(currentTile: StateFlow<Long?>, grid: HexGrid, builtCells: Flow<Set<String>>) =
    viewModelFactory {
        initializer {
            HexGridViewModel(currentTile = currentTile, grid = grid, builtCells = builtCells)
        }
    }

/**
 * Fabrique le [TileInspectionViewModel] en câblant la **même** intégration H3 partagée ([grid]) et la
 * **même** tuile courante partagée ([currentTile]) que la grille. Le contenu d'une tuile vient du
 * [WorldGenerator] de `:domain`, instancié sur cette grille (elle joue le rôle de
 * [com.hexa.world.TileCenterLocator]) : la résolution tap → cellule et le générateur partagent ainsi
 * une seule façade native.
 */
private fun tileInspectionViewModelFactory(grid: HexGrid, currentTile: StateFlow<Long?>) = viewModelFactory {
    initializer {
        TileInspectionViewModel(
            grid = grid,
            contentOf = WorldGenerator(centerLocator = grid)::contentOf,
            currentTile = currentTile,
        )
    }
}
