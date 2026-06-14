package com.hexa.location

import com.hexa.location.HeadingSmoother.smoothedHeading
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

/**
 * [HeadingSmoother] lisse un cap (angle en degrés) par moyenne mobile exponentielle **circulaire** :
 * il interpole le long du plus court arc entre l'ancien cap et le brut, ce qui élimine l'à-coup au
 * passage 359°→0°. Le cœur de cette logique — et le seul endroit subtil — est le franchissement de
 * la couture 0/360, qu'on couvre explicitement.
 */
class HeadingSmootherTest : StringSpec({
    val eps = 1e-9

    "un facteur de 1 adopte immédiatement le cap brut" {
        HeadingSmoother.smooth(previousDeg = 10.0, rawDeg = 80.0, factor = 1.0) shouldBe
            (80.0 plusOrMinus eps)
    }

    "un facteur de 0 conserve l'ancien cap" {
        HeadingSmoother.smooth(previousDeg = 10.0, rawDeg = 80.0, factor = 0.0) shouldBe
            (10.0 plusOrMinus eps)
    }

    "un facteur intermédiaire interpole entre les deux caps" {
        HeadingSmoother.smooth(previousDeg = 0.0, rawDeg = 90.0, factor = 0.5) shouldBe
            (45.0 plusOrMinus eps)
    }

    "interpole par le plus court arc en montant à travers 0° (359°→1°)" {
        // Le chemin court va de 359° à 1° (+2°), pas de 359° vers 1° à reculons (−358°).
        HeadingSmoother.smooth(previousDeg = 359.0, rawDeg = 1.0, factor = 0.5) shouldBe
            (0.0 plusOrMinus eps)
    }

    "interpole par le plus court arc en descendant à travers 0° (1°→359°)" {
        HeadingSmoother.smooth(previousDeg = 1.0, rawDeg = 359.0, factor = 0.5) shouldBe
            (0.0 plusOrMinus eps)
    }

    "normalise toujours le résultat dans [0, 360)" {
        HeadingSmoother.smooth(previousDeg = 350.0, rawDeg = 10.0, factor = 0.5) shouldBe
            (0.0 plusOrMinus eps)
    }

    "rejette un facteur hors de [0, 1]" {
        shouldThrow<IllegalArgumentException> {
            HeadingSmoother.smooth(previousDeg = 0.0, rawDeg = 90.0, factor = 1.5)
        }
    }

    "l'opérateur de flux émet le premier cap tel quel puis lisse les suivants" {
        val smoothed = flowOf(0.0, 90.0, 90.0).smoothedHeading(factor = 0.5).toList()
        smoothed[0] shouldBe (0.0 plusOrMinus eps) // amorçage : premier cap brut
        smoothed[1] shouldBe (45.0 plusOrMinus eps) // 0 → 90 à 0,5
        smoothed[2] shouldBe (67.5 plusOrMinus eps) // 45 → 90 à 0,5
    }

    "l'opérateur de flux franchit la couture 0/360 sans à-coup" {
        val smoothed = flowOf(359.0, 1.0).smoothedHeading(factor = 0.5).toList()
        smoothed[1] shouldBe (0.0 plusOrMinus eps)
    }
})
