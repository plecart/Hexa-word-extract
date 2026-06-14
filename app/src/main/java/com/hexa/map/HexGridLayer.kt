package com.hexa.map

import com.hexa.core.geo.LatLng
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant les contours de la grille. */
private const val GRID_SOURCE_ID = "hexa-grid-source"

/** Identifiant de la couche de lignes traçant la grille. */
private const val GRID_LAYER_ID = "hexa-grid-layer"

/**
 * Met à jour la surcouche de grille hexagonale du style avec les [outlines] courants.
 *
 * Au premier appel, crée la source GeoJSON et la couche de lignes ; ensuite, ne fait que **réinjecter
 * les données** dans la source existante — bien moins coûteux que de recréer la couche, pour rester
 * fluide quand le joueur change de cellule. Chaque contour est fermé (premier sommet répété) afin que
 * l'hexagone apparaisse entier.
 *
 * @param outlines un anneau de sommets par cellule à dessiner (vide tant qu'aucune position connue).
 */
internal fun Style.showHexGrid(outlines: List<List<LatLng>>) {
    val features = gridFeatures(outlines)
    getSourceAs<GeoJsonSource>(GRID_SOURCE_ID)?.let { source ->
        source.featureCollection(features)
        return
    }
    addSource(geoJsonSource(GRID_SOURCE_ID) { featureCollection(features) })
    if (getLayer(GRID_LAYER_ID) == null) {
        addLayer(
            lineLayer(GRID_LAYER_ID, GRID_SOURCE_ID) {
                lineColor(MapConfig.GRID_LINE_COLOR)
                lineWidth(MapConfig.GRID_LINE_WIDTH)
                lineOpacity(MapConfig.GRID_LINE_OPACITY)
            },
        )
    }
}

/** Convertit les contours de cellules en collection de lignes fermées, prête pour la source GeoJSON. */
private fun gridFeatures(outlines: List<List<LatLng>>): FeatureCollection = FeatureCollection.fromFeatures(
    outlines.map { ring ->
        val points = ring.map { Point.fromLngLat(it.lngDeg, it.latDeg) }
        Feature.fromGeometry(LineString.fromLngLats(points + points.first()))
    },
)
