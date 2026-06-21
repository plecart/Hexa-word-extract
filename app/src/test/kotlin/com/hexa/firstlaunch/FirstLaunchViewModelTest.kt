@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.firstlaunch

import com.hexa.core.geo.LatLng
import com.hexa.location.PositionSource
import com.hexa.map.HexGrid
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * [FirstLaunchViewModel] est la glu de l'écran de premier lancement : il suit la **tuile courante**
 * (pour n'activer le bouton que lorsqu'elle est connue) et, à la demande, **délègue** la pose en
 * convertissant la tuile en son index H3 textuel. On vérifie cette orchestration avec une fausse
 * grille et une source de position factice — sans device, sans Firebase.
 */
class FirstLaunchViewModelTest : StringSpec({
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "la tuile courante est inconnue tant qu'aucune position n'est reçue" {
        runTest {
            val vm = FirstLaunchViewModel(PositionSource { emptyFlow() }, FakeGrid(), placeBaseAt = {})
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.currentTile.value shouldBe null
        }
    }

    "la tuile courante suit la position reçue" {
        runTest {
            val vm =
                FirstLaunchViewModel(
                    PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) },
                    FakeGrid(),
                    placeBaseAt = {},
                )
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.currentTile.value shouldBe 48L
        }
    }

    "poser la base délègue avec l'index H3 textuel de la tuile courante" {
        runTest {
            val placed = mutableListOf<String>()
            val vm =
                FirstLaunchViewModel(
                    PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) },
                    FakeGrid(),
                    placeBaseAt = { placed += it },
                )
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.placeBase()
            advanceUntilIdle()

            placed shouldBe listOf("h3-48")
        }
    }

    "poser la base ne fait rien tant que la tuile courante est inconnue" {
        runTest {
            val placed = mutableListOf<String>()
            val vm = FirstLaunchViewModel(PositionSource { emptyFlow() }, FakeGrid(), placeBaseAt = { placed += it })
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.placeBase()
            advanceUntilIdle()

            placed.shouldBeEmpty()
        }
    }
})

/** Un collecteur de fond active le suivi de tuile (StateFlow `WhileSubscribed`) le temps du test. */
private fun CoroutineScope.collectCurrentTile(vm: FirstLaunchViewModel) = launch { vm.currentTile.collect {} }

/** Fausse grille : la cellule est la latitude tronquée, et son index textuel est `h3-<cell>`. */
private class FakeGrid : HexGrid {
    override fun cellAt(position: LatLng): Long = position.latDeg.toLong()

    override fun centerOf(h3Index: Long): LatLng = LatLng(h3Index.toDouble(), 0.0)

    override fun toH3String(cell: Long): String = "h3-$cell"

    override fun disk(center: Long, rings: Int): List<Long> = error("non utilisé")

    override fun outline(cell: Long): List<LatLng> = error("non utilisé")
}
