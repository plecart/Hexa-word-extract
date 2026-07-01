@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.map

import com.hexa.core.geo.LatLng
import com.hexa.map.CurrentTileTracker.currentTile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

/**
 * [CurrentTileTracker] est le module **pur** de suivi de la tuile courante : il transforme une suite
 * de positions lissées en cellule H3 courante avec **hystérésis** — on ne bascule sur une nouvelle
 * cellule que si le joueur est *franchement* à l'intérieur (centre franchement plus proche, d'au
 * moins la marge). On le teste sans device ni H3, avec une grille linéaire factice où l'on maîtrise
 * la frontière, pour prouver l'absence de bascule sur une trace qui oscille autour d'elle (AC2).
 */
class CurrentTileTrackerTest : StringSpec({
    // Deux cellules A et B distantes d'environ 111 m, frontière au milieu (lng = 0,0005).
    val grid = LinearFakeGrid(boundaryLng = 0.0005, centerA = LatLng(0.0, 0.0), centerB = LatLng(0.0, 0.001))
    val marginM = 20.0

    // Un pas du tracker depuis la tuile [current] vers une position située à la longitude [lng] (lat 0).
    fun step(current: Long?, lng: Double): Long = CurrentTileTracker.next(current, LatLng(0.0, lng), grid, marginM)

    "la première position fixe la tuile courante sur sa cellule" {
        step(current = null, lng = 0.0) shouldBe LinearFakeGrid.CELL_A
    }

    "rester dans la même cellule ne change pas la tuile courante" {
        step(LinearFakeGrid.CELL_A, lng = 0.0002) shouldBe LinearFakeGrid.CELL_A
    }

    "franchir à peine la frontière ne bascule pas la tuile (hystérésis)" {
        // Juste au-delà de la frontière : géométriquement dans B, mais pas plus proche de B d'une marge.
        step(LinearFakeGrid.CELL_A, lng = 0.00051) shouldBe LinearFakeGrid.CELL_A
    }

    "entrer franchement dans la nouvelle cellule bascule la tuile" {
        // Bien à l'intérieur de B : centre de B franchement plus proche que celui de A.
        step(LinearFakeGrid.CELL_A, lng = 0.0009) shouldBe LinearFakeGrid.CELL_B
    }

    "une trace qui oscille autour de la frontière ne fait jamais clignoter la tuile courante" {
        runTest {
            // Oscillation serrée de part et d'autre de la frontière (0,0005), toujours à moins d'une marge.
            val trace = flowOf(
                LatLng(0.0, 0.00045),
                LatLng(0.0, 0.00052),
                LatLng(0.0, 0.00048),
                LatLng(0.0, 0.00053),
                LatLng(0.0, 0.00047),
            )
            trace.currentTile(grid, marginM).toList() shouldBe listOf(LinearFakeGrid.CELL_A)
        }
    }

    "un déplacement franc d'une cellule à l'autre émet la nouvelle tuile une seule fois" {
        runTest {
            // A, puis franchement dans B, puis toujours B.
            val trace = flowOf(
                LatLng(0.0, 0.0000),
                LatLng(0.0, 0.0009),
                LatLng(0.0, 0.0010),
            )
            trace.currentTile(grid, marginM).toList() shouldBe listOf(LinearFakeGrid.CELL_A, LinearFakeGrid.CELL_B)
        }
    }
})

/**
 * Grille factice **1-D** : la cellule dépend du seul franchissement d'une frontière en longitude, et
 * chaque cellule a un centre fixe connu. Permet de piloter exactement la géométrie « de part et
 * d'autre de la frontière » dont dépend l'hystérésis, sans la bibliothèque H3.
 */
private class LinearFakeGrid(
    private val boundaryLng: Double,
    private val centerA: LatLng,
    private val centerB: LatLng,
) : HexGrid {
    override fun cellAt(position: LatLng): Long = if (position.lngDeg < boundaryLng) CELL_A else CELL_B

    override fun centerOf(h3Index: Long): LatLng = if (h3Index == CELL_A) centerA else centerB

    override fun disk(center: Long, rings: Int): List<Long> = listOf(center)

    override fun outline(cell: Long): List<LatLng> = emptyList()

    override fun toH3String(cell: Long): String = cell.toString()

    override fun gridDistance(a: Long, b: Long): Int = error("non utilisé")

    companion object {
        const val CELL_A = 1L
        const val CELL_B = 2L
    }
}
