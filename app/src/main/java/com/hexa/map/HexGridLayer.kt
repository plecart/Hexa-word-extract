package com.hexa.map

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant les cellules de la grille (contour + état). */
private const val GRID_SOURCE_ID = "hexa-grid-source"

/** Identifiant de la couche de remplissage qui distingue les tuiles courante et bâtie. */
private const val GRID_FILL_LAYER_ID = "hexa-grid-fill-layer"

/** Identifiant de la couche de lignes traçant le contour de chaque cellule. */
private const val GRID_LINE_LAYER_ID = "hexa-grid-line-layer"

/** Propriété GeoJSON portant l'état visuel de la cellule ([TileState.name]). */
private const val STATE_PROPERTY = "state"

/**
 * Met à jour la surcouche de grille hexagonale du style avec les [cells] courantes.
 *
 * Au premier appel, crée la source GeoJSON puis **deux couches** : un remplissage piloté par l'état
 * de chaque cellule (la tuile courante et les tuiles bâties reçoivent une teinte distincte, les
 * autres restent transparentes) sous un contour de ligne tracé pour toutes les cellules. Ensuite, ne
 * fait que **réinjecter les données** dans la source existante — bien moins coûteux que de recréer
 * les couches, pour rester fluide quand le joueur change de cellule. Chaque cellule est un polygone
 * fermé : le remplissage en colore l'intérieur, la ligne en souligne le pourtour.
 *
 * @param cells une cellule (contour + état) par hexagone à dessiner (vide tant qu'aucune position
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
        // Remplissage d'abord (sous la ligne) : sa couleur dépend de l'état de la cellule.
        addLayer(
            fillLayer(GRID_FILL_LAYER_ID, GRID_SOURCE_ID) {
                fillColor(tileFillColorByState())
            },
        )
    }
    if (getLayer(GRID_LINE_LAYER_ID) == null) {
        addLayer(
            lineLayer(GRID_LINE_LAYER_ID, GRID_SOURCE_ID) {
                lineColor(MapConfig.GRID_LINE_COLOR)
                lineWidth(MapConfig.GRID_LINE_WIDTH)
                lineOpacity(MapConfig.GRID_LINE_OPACITY)
            },
        )
    }
}

/**
 * Expression Mapbox « data-driven » associant à chaque état de cellule sa couleur de remplissage :
 * teinte de la tuile courante, teinte des tuiles bâties, transparent pour les cellules normales
 * (valeur par défaut). Les alphas sont portés par les couleurs `rgba` de [MapConfig].
 */
private fun tileFillColorByState(): Expression = Expression.match {
    get { literal(STATE_PROPERTY) }
    literal(TileState.COURANTE.name)
    literal(MapConfig.TILE_CURRENT_FILL_COLOR)
    literal(TileState.BATIE.name)
    literal(MapConfig.TILE_BUILT_FILL_COLOR)
    literal(MapConfig.TILE_NORMAL_FILL_COLOR)
}

/** Convertit les cellules en polygones fermés annotés de leur état, prêts pour la source GeoJSON. */
private fun gridFeatures(cells: List<GridCell>): FeatureCollection = FeatureCollection.fromFeatures(
    cells.map { cell ->
        val points = cell.outline.map { Point.fromLngLat(it.lngDeg, it.latDeg) }
        val closedRing = points + points.first()
        Feature.fromGeometry(Polygon.fromLngLats(listOf(closedRing))).apply {
            addStringProperty(STATE_PROPERTY, cell.state.name)
        }
    },
)
