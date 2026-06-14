package com.hexa.map

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.google.android.gms.location.LocationServices
import com.hexa.R
import com.hexa.location.CameraMode
import com.hexa.location.ChaseCameraConfig
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
    val viewModel: ChaseCameraViewModel = viewModel(factory = chaseCameraViewModelFactory(context))

    val camera by viewModel.cameraState.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()

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
 * Fabrique le [ChaseCameraViewModel] en câblant les sources concrètes : position GPS filtrée
 * ([FusedLocationSource]) et boussole de l'appareil ([CompassHeadingSource]) pour le cap, avec les
 * réglages de [MapConfig]. La permission de localisation est garantie en amont par
 * [LocationPermissionGate].
 */
private fun chaseCameraViewModelFactory(context: Context) = viewModelFactory {
    initializer {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        ChaseCameraViewModel(
            positionSource = FusedLocationSource(
                client = LocationServices.getFusedLocationProviderClient(context),
                intervalMs = MapConfig.GPS_INTERVAL_MS,
                smoothingFactor = MapConfig.POSITION_SMOOTHING_FACTOR,
                accuracyThresholdM = MapConfig.ACCURACY_THRESHOLD_M,
            ),
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
