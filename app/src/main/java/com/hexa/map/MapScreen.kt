package com.hexa.map

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
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
import com.hexa.location.CameraMode
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.PositionSource
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
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.gestures
import com.uber.h3core.H3Core

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
 */
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    LocationPermissionGate(modifier = modifier) {
        ChaseCameraMap(modifier = Modifier.fillMaxSize())
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
private fun ChaseCameraMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val positionSource = (context.applicationContext as HexaApplication).sharedPositionSource
    val viewModel: ChaseCameraViewModel = viewModel(factory = chaseCameraViewModelFactory(context, positionSource))
    val gridViewModel: HexGridViewModel = viewModel(factory = hexGridViewModelFactory(positionSource))

    val camera by viewModel.cameraState.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val gridOutlines by gridViewModel.outlines.collectAsStateWithLifecycle()

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
            // Redessine la grille hexagonale à chaque nouvel ensemble de contours (changement de
            // cellule ou de palier de zoom) ; la source GeoJSON n'est créée qu'une fois.
            MapEffect(gridOutlines) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showHexGrid(gridOutlines) }
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

        if (mode == CameraMode.FREE) {
            ExtendedFloatingActionButton(
                onClick = viewModel::recenter,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Text(text = stringResource(R.string.recenter_camera))
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
 * Fabrique le [HexGridViewModel] en câblant la **même** position GPS filtrée partagée que la caméra
 * ([positionSource]) et l'intégration H3 de production ([H3Grid]) : un seul abonnement GPS sert la
 * caméra et la grille.
 */
private fun hexGridViewModelFactory(positionSource: PositionSource) = viewModelFactory {
    initializer {
        // Sur Android, H3 se charge depuis les jniLibs via newSystemInstance (cf. [H3Grid]).
        HexGridViewModel(positionSource = positionSource, grid = H3Grid(h3 = H3Core.newSystemInstance()))
    }
}
