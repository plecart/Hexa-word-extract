@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.location

import com.hexa.core.geo.LatLng
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

/**
 * [SimulatedTrajectoryPositionSource] est le bouchon de position de cette tranche : il rejoue un
 * trajet factice, un point à la fois, en attendant un pas entre chaque. On vérifie l'ordre des
 * points, la cadence (en temps virtuel) et le rebouclage. Le GPS réel (#10) le remplacera derrière
 * le même contrat [PositionSource].
 */
class SimulatedTrajectoryPositionSourceTest : StringSpec({
    val trajectory = listOf(
        LatLng(48.8566, 2.3522),
        LatLng(48.8570, 2.3530),
        LatLng(48.8575, 2.3540),
    )

    "rejoue les points du trajet dans l'ordre, sans boucler" {
        runTest {
            val source = SimulatedTrajectoryPositionSource(trajectory, step = 1.seconds, loop = false)
            source.positions().toList() shouldBe trajectory
        }
    }

    "attend un pas entre deux points consécutifs" {
        runTest {
            val source = SimulatedTrajectoryPositionSource(trajectory, step = 1.seconds, loop = false)
            source.positions().take(3).toList()
            // Trois points → deux intervalles d'un pas, sans délai avant le premier ni après le dernier.
            testScheduler.currentTime shouldBe 2_000L
        }
    }

    "reboucle sur le trajet quand loop est actif" {
        runTest {
            val source = SimulatedTrajectoryPositionSource(trajectory, step = 1.seconds, loop = true)
            source.positions().take(5).toList() shouldBe
                listOf(trajectory[0], trajectory[1], trajectory[2], trajectory[0], trajectory[1])
        }
    }

    "rejette un trajet vide" {
        shouldThrow<IllegalArgumentException> {
            SimulatedTrajectoryPositionSource(emptyList(), step = 1.seconds)
        }
    }

    "rejette un pas non strictement positif" {
        shouldThrow<IllegalArgumentException> {
            SimulatedTrajectoryPositionSource(trajectory, step = 0.seconds)
        }
    }
})
