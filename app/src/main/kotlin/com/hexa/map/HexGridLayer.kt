package com.hexa.map

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant les cellules de la grille (polygones + teinte). */
private const val GRID_SOURCE_ID = "hexa-grid-source"

/** Identifiant de la couche de remplissage colorant chaque cellule selon sa teinte. */
private const val GRID_FILL_LAYER_ID = "hexa-grid-fill-layer"

/** Propriété GeoJSON portant la teinte de remplissage de la cellule ([GridCell.fillColorRgba]). */
private const val FILL_COLOR_PROPERTY = "fillColor"

/**
 * Met à jour la grille hexagonale du style avec les [cells] courantes.
 *
 * Au premier appel, crée la source GeoJSON puis une **couche de remplissage** dont la couleur est lue
 * par cellule ([FILL_COLOR_PROPERTY], data-driven) : chaque hexagone reçoit sa propre teinte (élément
 * le plus rare, neutre, ou surlignage de la tuile courante — cf. [tileFillColor]), l'alpha étant porté
 * par la couleur `rgba`. Ensuite, ne fait que **réinjecter les données** dans la source existante —
 * bien moins coûteux que de recréer la couche, pour rester fluide quand le joueur change de cellule.
 * **Aucun contour n'est tracé** : l'identité visuelle d'une tuile passe uniquement par son remplissage.
 *
 * Calque la mécanique de [Style.showBuildingModels] (source créée une fois, couleur data-driven par
 * feature, données réinjectées ensuite).
 *
 * @param cells une cellule (contour + teinte) par hexagone à dessiner (vide tant qu'aucune position
 *   n'est connue).
 */
internal fun Style.showHexGrid(cells: List<GridCell>) {
    val features = gridFeatures(cells)
    getSourceAs<GeoJsonSource>(GRID_SOURCE_ID)?.let { source ->
        source.featureCollection(features)
        return
    }
    addSource(geoJsonSource(GRID_SOURCE_ID) { featureCollection(features) })
    if (getLayer(GRID_FILL_LAYER_ID) == null) {
        addLayer(
            fillLayer(GRID_FILL_LAYER_ID, GRID_SOURCE_ID) {
                fillColor(Expression.get(FILL_COLOR_PROPERTY))
            },
        )
    }
}

/** Convertit les cellules en polygones fermés annotés de leur teinte, prêts pour la source GeoJSON. */
private fun gridFeatures(cells: List<GridCell>): FeatureCollection = FeatureCollection.fromFeatures(
    cells.map { cell ->
        val points = cell.outline.map { Point.fromLngLat(it.lngDeg, it.latDeg) }
        val closedRing = points + points.first()
        Feature.fromGeometry(Polygon.fromLngLats(listOf(closedRing))).apply {
            addStringProperty(FILL_COLOR_PROPERTY, cell.fillColorRgba)
        }
    },
)
