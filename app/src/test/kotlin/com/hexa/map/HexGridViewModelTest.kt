@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.map

import com.hexa.config.Element
import com.hexa.core.geo.LatLng
import com.hexa.world.TileContent
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
import kotlin.math.abs

/**
 * [HexGridViewModel] est la glu d'app de la grille : à partir de la tuile courante partagée, il déduit
 * le disque de cellules au zoom courant et expose chacune avec sa **teinte de remplissage** (état +
 * contenu, cf. [tileFillColor]). On vérifie cette orchestration avec une fausse grille et un faux
 * générateur de contenu — sans device ni H3 —, en s'assurant que la grille ne se recalcule que lorsque
 * la **tuile courante** ou le **palier de zoom** change, et que chaque cellule reçoit la bonne teinte.
 * Le suivi de la tuile courante (hystérésis) est testé à part (`CurrentTileTrackerTest`), le mapping
 * couleur aussi (`TileFillTest`).
 */
class HexGridViewModelTest : StringSpec({
    // viewModelScope tourne sur Dispatchers.Main : on le branche sur le planificateur de test.
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "expose le disque de rayon fixe autour de la cellule courante, indépendant du zoom" {
        runTest {
            val grid = FakeHexGrid()
            val vm = HexGridViewModel(MutableStateFlow(48L), grid, emptyContent)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()

            grid.lastCenter shouldBe 48L
            grid.lastRings shouldBe MapConfig.GRID_RENDER_RINGS
            vm.cells.value shouldHaveSize MapConfig.GRID_RENDER_RINGS
            // Chaque cellule du disque devient une GridCell ; la première est celle de la cellule courante.
            vm.cells.value.first().outline shouldBe listOf(LatLng(48.0, 0.0))
        }
    }

    "teinte chaque cellule selon son contenu et l'estompe selon sa distance au joueur" {
        runTest {
            val grid = FakeHexGrid()
            // La cellule courante (48, distance 0) et sa voisine (49, distance 1) portent chacune un
            // gisement : toutes deux teintées par leur élément (la courante non distinguée), mais la
            // voisine est atténuée par le fondu-distance.
            val content = mapOf(48L to tile(Element.CENDRITE), 49L to tile(Element.ECHOFER))
            val vm = HexGridViewModel(MutableStateFlow(48L), grid) { content[it] ?: tile() }
            backgroundScope.launchCells(vm)
            advanceUntilIdle()

            val byCell = vm.cells.value.associateBy { it.outline.first().latDeg.toLong() }
            byCell[48L]?.fillColorRgba shouldBe tileFillColor(tile(Element.CENDRITE), distanceRings = 0)
            byCell[49L]?.fillColorRgba shouldBe tileFillColor(tile(Element.ECHOFER), distanceRings = 1)
        }
    }

    "recalcule la grille quand la tuile courante change" {
        runTest {
            val grid = FakeHexGrid()
            val currentTile = MutableStateFlow<Long?>(48L)
            val vm = HexGridViewModel(currentTile, grid, emptyContent)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()
            grid.lastCenter shouldBe 48L

            currentTile.value = 49L
            advanceUntilIdle()

            grid.lastCenter shouldBe 49L
        }
    }

    "ne recalcule pas la grille tant que la tuile courante ne change pas (même valeur ré-émise)" {
        runTest {
            val grid = FakeHexGrid()
            val currentTile = MutableStateFlow<Long?>(48L)
            val vm = HexGridViewModel(currentTile, grid, emptyContent)
            backgroundScope.launchCells(vm)
            advanceUntilIdle()
            val calls = grid.diskCalls

            // La même tuile ré-émise ne déclenche aucun recalcul (grille figée sur la tuile courante).
            currentTile.value = 48L
            advanceUntilIdle()

            grid.diskCalls shouldBe calls
        }
    }
})

/** Générateur de contenu neutre : toutes les tuiles vides (la coloration n'est pas le sujet du test). */
private val emptyContent: (Long) -> TileContent = { tile() }

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

    override fun toH3String(cell: Long): String = cell.toString()

    // Distance déterministe cohérente avec `disk` (cellules contiguës center+i) : l'écart d'index.
    override fun gridDistance(a: Long, b: Long): Int = abs(a - b).toInt()
}
