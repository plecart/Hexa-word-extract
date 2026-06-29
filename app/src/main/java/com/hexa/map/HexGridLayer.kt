package com.hexa.map

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant les cellules de la grille (polygones + état). */
private const val GRID_SOURCE_ID = "hexa-grid-source"

/** Propriété GeoJSON portant l'état visuel de la cellule ([TileState.name]). */
private const val STATE_PROPERTY = "state"

/**
 * Met à jour la **source** GeoJSON de la grille hexagonale du style avec les [cells] courantes.
 *
 * Au premier appel, crée la source ; ensuite, ne fait que **réinjecter les données** dans la source
 * existante — bien moins coûteux que de la recréer, pour rester fluide quand le joueur change de
 * cellule. Chaque cellule est un polygone fermé annoté de son [TileState] ([STATE_PROPERTY]).
 *
 * **Aucune couche visible n'est posée ici** : #125 a retiré l'habillage cyan (contour + remplissage
 * de la tuile courante) ; la grille n'affiche donc temporairement plus rien. Cette source est
 * **conservée comme fondation** — #126 (recoloration des hexagones) y rebranchera une couche de
 * remplissage pilotée par l'état et le contenu de chaque cellule, sans recréer ce câblage.
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
