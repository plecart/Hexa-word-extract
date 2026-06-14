@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.map

import com.hexa.core.geo.LatLng
import com.hexa.location.PositionSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * [HexGridViewModel] est la glu d'app de la grille : il transforme la position filtrée en cellule
 * courante, en déduit le disque de cellules au zoom courant et expose leurs contours. On vérifie cette
 * orchestration avec une fausse grille — sans device ni H3 —, en s'assurant que la grille ne se
 * recalcule que lorsque la **cellule** ou le **palier de zoom** change.
 */
class HexGridViewModelTest : StringSpec({
    // viewModelScope tourne sur Dispatchers.Main : on le branche sur le planificateur de test.
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "expose les contours du disque autour de la cellule courante, au rayon du zoom de poursuite" {
        runTest {
            val grid = FakeHexGrid()
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid)
            backgroundScope.launchOutlines(vm)
            advanceUntilIdle()

            grid.lastCenter shouldBe 48L
            grid.lastRings shouldBe VisibleCells.ringsForZoom(MapConfig.FOLLOW_ZOOM)
            vm.outlines.value shouldHaveSize VisibleCells.ringsForZoom(MapConfig.FOLLOW_ZOOM)
            // Chaque cellule du disque devient un contour : le premier est celui de la cellule courante.
            vm.outlines.value.first() shouldBe listOf(LatLng(48.0, 0.0))
        }
    }

    "recalcule la grille quand le joueur change de cellule" {
        runTest {
            val grid = FakeHexGrid()
            val position = MutableStateFlow(LatLng(48.0, 2.0))
            val vm = HexGridViewModel(PositionSource { position }, grid)
            backgroundScope.launchOutlines(vm)
            advanceUntilIdle()

            position.value = LatLng(49.0, 2.0)
            advanceUntilIdle()

            grid.lastCenter shouldBe 49L
        }
    }

    "ne recalcule pas la grille tant que le zoom reste dans le même palier" {
        runTest {
            val grid = FakeHexGrid()
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid)
            backgroundScope.launchOutlines(vm)
            advanceUntilIdle()
            // On se cale d'abord dans le palier ≥ 18 (2 anneaux), puis on observe.
            vm.onZoomChanged(19.0)
            advanceUntilIdle()
            val callsInPalier = grid.diskCalls

            // Un autre zoom du même palier (≥ 18 → 2 anneaux) : aucun recalcul.
            vm.onZoomChanged(18.5)
            advanceUntilIdle()

            grid.diskCalls shouldBe callsInPalier
        }
    }

    "recalcule la grille quand le zoom franchit un palier" {
        runTest {
            val grid = FakeHexGrid()
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid)
            backgroundScope.launchOutlines(vm)
            advanceUntilIdle()

            vm.onZoomChanged(MapConfig.MAX_ZOOM)
            advanceUntilIdle()

            grid.lastRings shouldBe MapConfig.GRID_MIN_RINGS
            vm.outlines.value shouldHaveSize MapConfig.GRID_MIN_RINGS
        }
    }
})

/** Un collecteur de fond active la chaîne (StateFlow `WhileSubscribed`) le temps du test. */
private fun CoroutineScope.launchOutlines(vm: HexGridViewModel) = launch { vm.outlines.collect {} }

/**
 * Fausse grille déterministe : la cellule est la latitude tronquée, le disque renvoie `rings` cellules
 * contiguës et chaque contour porte l'index de sa cellule, pour des assertions simples. Compte les
 * appels à `disk` afin de vérifier que la grille ne se recalcule qu'au bon moment.
 */
private class FakeHexGrid : HexGrid {
    var lastCenter: Long? = null
    var lastRings: Int? = null
    var diskCalls = 0

    override fun cellAt(position: LatLng): Long = position.latDeg.toLong()

    override fun disk(center: Long, rings: Int): List<Long> {
        lastCenter = center
        lastRings = rings
        diskCalls++
        return List(rings) { center + it }
    }

    override fun outline(cell: Long): List<LatLng> = listOf(LatLng(cell.toDouble(), 0.0))

    override fun centerOf(h3Index: Long): LatLng = LatLng(h3Index.toDouble(), 0.0)
}
