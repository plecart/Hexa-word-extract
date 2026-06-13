package com.hexa.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle

/**
 * Carte Mapbox plein écran au style monochrome du projet ([MapConfig.STYLE_URL]).
 *
 * Au lancement, la carte est centrée sur la position par défaut ([MapConfig.DEFAULT_CENTER_LAT]/
 * [MapConfig.DEFAULT_CENTER_LON]) au zoom [MapConfig.DEFAULT_ZOOM]. Le déplacement au doigt et le
 * zoom au pincement sont assurés par les gestes natifs de Mapbox ; le zoom est borné à la plage de
 * jeu [MapConfig.MIN_ZOOM]–[MapConfig.MAX_ZOOM].
 *
 * Le token public est fourni au SDK en amont via `MapboxOptions.accessToken`
 * (cf. [com.hexa.MainActivity]).
 */
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    MapboxMap(
        modifier = modifier,
        mapViewportState =
        rememberMapViewportState {
            setCameraOptions {
                center(Point.fromLngLat(MapConfig.DEFAULT_CENTER_LON, MapConfig.DEFAULT_CENTER_LAT))
                zoom(MapConfig.DEFAULT_ZOOM)
            }
        },
        style = { MapStyle(style = MapConfig.STYLE_URL) },
    ) {
        // L'extension Compose n'expose pas les bornes de zoom via l'état de viewport : on les
        // applique sur la MapView sous-jacente, une fois, dès qu'elle est disponible.
        MapEffect(Unit) { mapView ->
            mapView.mapboxMap.setBounds(
                CameraBoundsOptions.Builder()
                    .minZoom(MapConfig.MIN_ZOOM)
                    .maxZoom(MapConfig.MAX_ZOOM)
                    .build(),
            )
        }
    }
}
