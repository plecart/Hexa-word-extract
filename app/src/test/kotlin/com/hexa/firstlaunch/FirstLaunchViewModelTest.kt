@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.firstlaunch

import com.hexa.core.geo.LatLng
import com.hexa.location.PositionSource
import com.hexa.map.HexGrid
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
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
            val vm = positionedViewModel(placeBaseAt = {})
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.currentTile.value shouldBe 48L
        }
    }

    "poser la base délègue avec l'index H3 textuel de la tuile courante" {
        runTest {
            val placed = mutableListOf<String>()
            val vm = positionedViewModel(placeBaseAt = { placed += it })
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

    "l'état placing suit le cycle false → true → false autour de la pose" {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = positionedViewModel(placeBaseAt = { gate.await() })
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.placing.value shouldBe false
            vm.placeBase()
            advanceUntilIdle()
            vm.placing.value shouldBe true

            gate.complete(Unit)
            advanceUntilIdle()
            vm.placing.value shouldBe false
        }
    }

    "poser à nouveau pendant une pose en cours n'envoie pas une seconde fois" {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val placed = mutableListOf<String>()
            val vm = positionedViewModel(placeBaseAt = {
                placed += it
                gate.await()
            })
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.placeBase()
            advanceUntilIdle()
            vm.placeBase()
            advanceUntilIdle()

            gate.complete(Unit)
            advanceUntilIdle()

            placed shouldBe listOf("h3-48")
        }
    }

    "une pose qui échoue rend l'échec visible et réarme la pose" {
        runTest {
            val vm = positionedViewModel(placeBaseAt = { error("persistance indisponible") })
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.placementFailed.value shouldBe false
            vm.placeBase()
            advanceUntilIdle()

            vm.placementFailed.value shouldBe true
            vm.placing.value shouldBe false
        }
    }

    "une nouvelle tentative efface l'échec précédent" {
        runTest {
            var attempts = 0
            val vm = positionedViewModel(placeBaseAt = { if (attempts++ == 0) error("échec transitoire") })
            backgroundScope.collectCurrentTile(vm)
            advanceUntilIdle()

            vm.placeBase()
            advanceUntilIdle()
            vm.placementFailed.value shouldBe true

            vm.placeBase()
            advanceUntilIdle()
            vm.placementFailed.value shouldBe false
        }
    }
})

/** ViewModel dont la position est fixée (tuile `48L`, index `h3-48`), pour piloter la pose. */
private fun positionedViewModel(placeBaseAt: suspend (String) -> Unit) =
    FirstLaunchViewModel(PositionSource { MutableStateFlow(LatLng(48.0, 2.0)) }, FakeGrid(), placeBaseAt = placeBaseAt)

/** Un collecteur de fond active le suivi de tuile (StateFlow `WhileSubscribed`) le temps du test. */
private fun CoroutineScope.collectCurrentTile(vm: FirstLaunchViewModel) = launch { vm.currentTile.collect {} }

/** Fausse grille : la cellule est la latitude tronquée, et son index textuel est `h3-<cell>`. */
private class FakeGrid : HexGrid {
    override fun cellAt(position: LatLng): Long = position.latDeg.toLong()

    override fun centerOf(h3Index: Long): LatLng = LatLng(h3Index.toDouble(), 0.0)

    override fun toH3String(cell: Long): String = "h3-$cell"

    override fun disk(center: Long, rings: Int): List<Long> = error("non utilisé")

    override fun outline(cell: Long): List<LatLng> = error("non utilisé")

    override fun gridDistance(a: Long, b: Long): Int = error("non utilisé")
}
