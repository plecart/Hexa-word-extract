package com.hexa.map

import com.hexa.core.geo.LatLng
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Grille à dessiner enfichée sur une fausse [HexGrid] : on teste la logique **pure** de sélection des
 * cellules visibles — combien d'anneaux pour un zoom donné, et que le disque est bien demandé autour
 * de la cellule courante — sans charger la bibliothèque native H3.
 */
class VisibleCellsTest : StringSpec({
    "le nombre d'anneaux décroît avec le zoom, entre les bornes configurées" {
        // Aux extrêmes de la plage de zoom : le plus large montre le plus d'anneaux, le plus serré le moins.
        VisibleCells.ringsForZoom(MapConfig.MAX_ZOOM) shouldBe MapConfig.GRID_MIN_RINGS
        VisibleCells.ringsForZoom(MapConfig.MIN_ZOOM) shouldBe MapConfig.GRID_MAX_RINGS
        // Au zoom de poursuite par défaut, le palier intermédiaire.
        VisibleCells.ringsForZoom(MapConfig.FOLLOW_ZOOM) shouldBe 3
    }

    "le nombre d'anneaux reste dans les bornes et ne croît jamais avec le zoom" {
        var previous = MapConfig.GRID_MAX_RINGS + 1
        var zoom = MapConfig.MIN_ZOOM
        while (zoom <= MapConfig.MAX_ZOOM) {
            val rings = VisibleCells.ringsForZoom(zoom)
            rings shouldBeGreaterThanOrEqual MapConfig.GRID_MIN_RINGS
            rings shouldBeLessThanOrEqual MapConfig.GRID_MAX_RINGS
            rings shouldBeLessThanOrEqual previous // monotonie : jamais plus d'anneaux en zoomant
            previous = rings
            zoom += 0.5
        }
    }

    "sous le zoom minimal, on retombe sur le nombre d'anneaux maximal (défensif)" {
        VisibleCells.ringsForZoom(MapConfig.MIN_ZOOM - 5.0) shouldBe MapConfig.GRID_MAX_RINGS
    }

    "les cellules visibles sont le disque demandé autour de la cellule courante, au rayon du zoom" {
        var askedCenter = -1L
        var askedRings = -1
        val grid = RecordingGrid { center, rings ->
            askedCenter = center
            askedRings = rings
            listOf(center, 1L, 2L)
        }

        val cells = VisibleCells.cellsAround(center = 42L, zoom = MapConfig.MAX_ZOOM, grid = grid)

        askedCenter shouldBe 42L
        askedRings shouldBe VisibleCells.ringsForZoom(MapConfig.MAX_ZOOM)
        cells shouldBe listOf(42L, 1L, 2L)
    }
})

/** Fausse grille qui n'observe que l'appel à `disk` ; les autres opérations sont inutiles ici. */
private class RecordingGrid(private val onDisk: (center: Long, rings: Int) -> List<Long>) : HexGrid {
    override fun cellAt(position: LatLng): Long = error("non utilisé")

    override fun disk(center: Long, rings: Int): List<Long> = onDisk(center, rings)

    override fun outline(cell: Long): List<LatLng> = error("non utilisé")

    override fun centerOf(h3Index: Long): LatLng = error("non utilisé")
}
