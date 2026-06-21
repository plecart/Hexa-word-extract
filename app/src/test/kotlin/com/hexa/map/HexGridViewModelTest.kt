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
 * courante (avec hystérésis), en déduit le disque de cellules au zoom courant et expose chacune avec
 * son état visuel. On vérifie cette orchestration avec une fausse grille — sans device ni H3 —, en
 * s'assurant que la grille ne se recalcule que lorsque la **cellule** ou le **palier de zoom** change,
 * et que chaque cellule reçoit le bon état (courante / bâtie / normale).
 */
class HexGridViewModelTest : StringSpec({
    // viewModelScope tourne sur Dispatchers.Main : on le branche sur le planificateur de test.
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "expose le disque autour de la cellule courante, au rayon du zoom de poursuite" {
        runTest {
            val grid = FakeHexGrid()
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid, NONE_BUILT)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()

            grid.lastCenter shouldBe 48L
            grid.lastRings shouldBe VisibleCells.ringsForZoom(MapConfig.FOLLOW_ZOOM)
            vm.cells.value shouldHaveSize VisibleCells.ringsForZoom(MapConfig.FOLLOW_ZOOM)
            // Chaque cellule du disque devient une GridCell ; la première est celle de la cellule courante.
            vm.cells.value.first().outline shouldBe listOf(LatLng(48.0, 0.0))
        }
    }

    "classe la cellule courante COURANTE, une bâtie BATIE, les autres NORMALE" {
        runTest {
            val grid = FakeHexGrid()
            // Le disque factice autour de 48 est [48, 49, 50, …] ; on marque 49 comme bâtie.
            val built = BuiltTiles { it == 49L }
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid, built)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()

            val byCell = vm.cells.value.associateBy { it.outline.first().latDeg.toLong() }
            byCell[48L]?.state shouldBe TileState.COURANTE
            byCell[49L]?.state shouldBe TileState.BATIE
            byCell[50L]?.state shouldBe TileState.NORMALE
        }
    }

    "recalcule la grille quand le joueur change franchement de cellule" {
        runTest {
            val grid = FakeHexGrid()
            val position = MutableStateFlow(LatLng(48.0, 2.0))
            val vm = HexGridViewModel(PositionSource { position }, grid, NONE_BUILT)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()

            // Un degré de latitude (~111 km) est très au-delà de la marge d'hystérésis : la tuile bascule.
            position.value = LatLng(49.0, 2.0)
            advanceUntilIdle()

            grid.lastCenter shouldBe 49L
        }
    }

    "ne recalcule pas la grille tant que le zoom reste dans le même palier" {
        runTest {
            val grid = FakeHexGrid()
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid, NONE_BUILT)
            backgroundScope.launchCells(vm)
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
            val vm = HexGridViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, grid, NONE_BUILT)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()

            vm.onZoomChanged(MapConfig.MAX_ZOOM)
            advanceUntilIdle()

            grid.lastRings shouldBe MapConfig.GRID_MIN_RINGS
            vm.cells.value shouldHaveSize MapConfig.GRID_MIN_RINGS
        }
    }
})

/** Aucune tuile bâtie : isole les tests qui ne portent pas sur l'état « bâtie ». */
private val NONE_BUILT = BuiltTiles { false }

/** Un collecteur de fond active la chaîne (StateFlow `WhileSubscribed`) le temps du test. */
private fun CoroutineScope.launchCells(vm: HexGridViewModel) = launch { vm.cells.collect {} }

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
