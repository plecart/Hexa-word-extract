@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.location

import com.hexa.core.geo.LatLng
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger

/**
 * [SharedPositionSource] partage un unique flux de positions chaud entre plusieurs consommateurs
 * (caméra de poursuite, grille hexagonale, avatar…) : on vérifie qu'il ne déclenche qu'**une seule**
 * souscription en amont — donc un seul abonnement GPS — quel que soit le nombre de collecteurs, et
 * qu'un collecteur arrivé en retard reçoit immédiatement la dernière position connue (replay).
 *
 * Les collecteurs tournent sur un [UnconfinedTestDispatcher] : ils s'abonnent dès leur lancement, si
 * bien que le partage [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed] démarre sans avoir à
 * piloter finement le temps virtuel.
 */
class SharedPositionSourceTest : StringSpec({
    val first = LatLng(48.8566, 2.3522)
    val next = LatLng(48.8570, 2.3530)

    "ne souscrit qu'une fois en amont quel que soit le nombre de collecteurs" {
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val upstreamSubscriptions = AtomicInteger(0)
            val upstream = MutableStateFlow(first)
            val delegate = PositionSource {
                flow {
                    upstreamSubscriptions.incrementAndGet()
                    emitAll(upstream)
                }
            }
            val shared = SharedPositionSource(delegate, scope)

            val seenByA = mutableListOf<LatLng>()
            val seenByB = mutableListOf<LatLng>()
            scope.launch { shared.positions().collect { seenByA += it } }
            scope.launch { shared.positions().collect { seenByB += it } }

            seenByA shouldBe listOf(first)
            seenByB shouldBe listOf(first)
            upstreamSubscriptions.get() shouldBe 1

            scope.cancel()
        }
    }

    "un collecteur tardif reçoit la dernière position connue" {
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val upstream = MutableStateFlow(first)
            val shared = SharedPositionSource(PositionSource { upstream }, scope)
            // Un premier collecteur maintient le partage actif pendant que la position évolue.
            scope.launch { shared.positions().collect {} }

            upstream.value = next

            shared.positions().first() shouldBe next

            scope.cancel()
        }
    }
})
