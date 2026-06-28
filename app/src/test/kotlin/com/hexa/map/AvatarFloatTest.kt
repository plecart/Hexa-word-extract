package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

/**
 * Verrouille la **fonction pure de flottement** de l'avatar : `offset(t) = amplitude · sin(2π·t/T)`.
 * C'est la frontière purement mathématique de l'effet fantôme (#98), vérifiable sans Mapbox ni
 * Android — on prouve ici le contrat (extrema, passage par le repos, périodicité, douceur aux
 * extrémités, bornes), le rendu Mapbox restant validé à l'œil sur device.
 */
class AvatarFloatTest : StringSpec({
    val amplitude = 2.0
    val period = 4000.0
    val tol = 1e-9

    "au départ (t=0) l'avatar est à sa position de repos : offset nul" {
        avatarFloatOffsetMeters(elapsedMs = 0.0, amplitudeM = amplitude, periodMs = period) shouldBe
            (0.0 plusOrMinus tol)
    }

    "au quart de période, l'avatar atteint son extremum haut (+amplitude)" {
        avatarFloatOffsetMeters(elapsedMs = period / 4, amplitudeM = amplitude, periodMs = period) shouldBe
            (amplitude plusOrMinus tol)
    }

    "à la demi-période, l'avatar repasse par le repos (offset nul)" {
        avatarFloatOffsetMeters(elapsedMs = period / 2, amplitudeM = amplitude, periodMs = period) shouldBe
            (0.0 plusOrMinus tol)
    }

    "aux trois quarts de période, l'avatar atteint son extremum bas (−amplitude)" {
        avatarFloatOffsetMeters(elapsedMs = 3 * period / 4, amplitudeM = amplitude, periodMs = period) shouldBe
            (-amplitude plusOrMinus tol)
    }

    "l'oscillation est périodique : t et t+T donnent le même offset" {
        listOf(0.0, period / 8, period / 3, 0.9 * period).forEach { t ->
            val here = avatarFloatOffsetMeters(t, amplitude, period)
            val nextCycle = avatarFloatOffsetMeters(t + period, amplitude, period)
            nextCycle shouldBe (here plusOrMinus tol)
        }
    }

    "l'offset reste borné par l'amplitude sur tout un cycle" {
        (0..1000).forEach { i ->
            val t = period * i / 1000.0
            val offset = avatarFloatOffsetMeters(t, amplitude, period)
            (abs(offset) <= amplitude + tol) shouldBe true
        }
    }

    "le mouvement est doux aux extrémités : la vitesse y est moindre qu'au passage par le repos" {
        val delta = period / 1000.0
        val nearRest = abs(
            avatarFloatOffsetMeters(delta, amplitude, period) - avatarFloatOffsetMeters(0.0, amplitude, period),
        )
        val nearPeak = abs(
            avatarFloatOffsetMeters(period / 4, amplitude, period) -
                avatarFloatOffsetMeters(period / 4 - delta, amplitude, period),
        )
        (nearPeak < nearRest) shouldBe true
    }

    "une période non positive ne produit aucun flottement (garde anti-division par zéro)" {
        avatarFloatOffsetMeters(elapsedMs = 1234.0, amplitudeM = amplitude, periodMs = 0.0) shouldBe 0.0
        avatarFloatOffsetMeters(elapsedMs = 1234.0, amplitudeM = amplitude, periodMs = -100.0) shouldBe 0.0
    }
})
