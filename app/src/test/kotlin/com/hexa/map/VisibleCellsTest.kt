package com.hexa.map

import com.hexa.core.geo.LatLng
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Grille à dessiner enfichée sur une fausse [HexGrid] : on teste la logique **pure** de sélection des
 * cellules visibles — le disque de **rayon fixe** [MapConfig.GRID_RENDER_RINGS] anneaux demandé autour
 * de la cellule courante, **indépendant du zoom** — sans charger la bibliothèque native H3.
 */
class VisibleCellsTest : StringSpec({
    "les cellules visibles sont le disque de rayon fixe autour de la cellule courante, quel que soit le zoom" {
        var askedCenter = -1L
        var askedRings = -1
        val grid = RecordingGrid { center, rings ->
            askedCenter = center
            askedRings = rings
            listOf(center, 1L, 2L)
        }

        val cells = VisibleCells.cellsAround(center = 42L, grid = grid)

        askedCenter shouldBe 42L
        askedRings shouldBe MapConfig.GRID_RENDER_RINGS
        cells shouldBe listOf(42L, 1L, 2L)
    }
})

/** Fausse grille qui n'observe que l'appel à `disk` ; les autres opérations sont inutiles ici. */
private class RecordingGrid(private val onDisk: (center: Long, rings: Int) -> List<Long>) : HexGrid {
    override fun cellAt(position: LatLng): Long = error("non utilisé")

    override fun disk(center: Long, rings: Int): List<Long> = onDisk(center, rings)

    override fun outline(cell: Long): List<LatLng> = error("non utilisé")

    override fun centerOf(h3Index: Long): LatLng = error("non utilisé")

    override fun toH3String(cell: Long): String = error("non utilisé")

    override fun gridDistance(a: Long, b: Long): Int = error("non utilisé")
}
