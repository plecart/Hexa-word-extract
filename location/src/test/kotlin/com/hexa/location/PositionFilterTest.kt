package com.hexa.location

import com.hexa.core.geo.LatLng
import com.hexa.location.PositionFilter.filteredPositions
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

/**
 * [PositionFilter] stabilise la position GPS : moyenne mobile exponentielle par axe (lat/lng) pour
 * supprimer le tremblement, et **rejet** des points dont la précision annoncée dépasse un seuil. Pur,
 * sans dépendance Android — testable avec des traces rejouées. On couvre les comportements exigés par
 * #10 : convergence du lissage, rejet des points imprécis, stabilité sur position immobile.
 */
class PositionFilterTest : StringSpec({
    val eps = 1e-9
    fun sample(lat: Double, lng: Double, accuracyM: Double = 5.0) = PositionSample(LatLng(lat, lng), accuracyM)

    "un facteur de 1 adopte immédiatement la position mesurée" {
        val out = PositionFilter.smooth(LatLng(48.0, 2.0), LatLng(49.0, 3.0), factor = 1.0)
        out.latDeg shouldBe (49.0 plusOrMinus eps)
        out.lngDeg shouldBe (3.0 plusOrMinus eps)
    }

    "un facteur de 0 conserve la position précédente" {
        val out = PositionFilter.smooth(LatLng(48.0, 2.0), LatLng(49.0, 3.0), factor = 0.0)
        out.latDeg shouldBe (48.0 plusOrMinus eps)
        out.lngDeg shouldBe (2.0 plusOrMinus eps)
    }

    "un facteur intermédiaire interpole chaque axe" {
        val out = PositionFilter.smooth(LatLng(48.0, 2.0), LatLng(50.0, 4.0), factor = 0.5)
        out.latDeg shouldBe (49.0 plusOrMinus eps)
        out.lngDeg shouldBe (3.0 plusOrMinus eps)
    }

    "smooth rejette un facteur hors de [0, 1]" {
        shouldThrow<IllegalArgumentException> {
            PositionFilter.smooth(LatLng(48.0, 2.0), LatLng(49.0, 3.0), factor = 1.5)
        }
    }

    "le flux émet la première position acceptée telle quelle puis lisse les suivantes" {
        val out = flowOf(sample(48.0, 2.0), sample(50.0, 4.0))
            .filteredPositions(smoothingFactor = 0.5, accuracyThresholdM = 20.0)
            .toList()
        out[0] shouldBe LatLng(48.0, 2.0) // amorçage
        out[1].latDeg shouldBe (49.0 plusOrMinus eps) // 48 → 50 à 0,5
        out[1].lngDeg shouldBe (3.0 plusOrMinus eps)
    }

    "le flux rejette les points dont la précision dépasse le seuil" {
        // Le point du milieu est imprécis (accuracyM = 50 > seuil 20) → ignoré.
        val out = flowOf(
            sample(48.0, 2.0, accuracyM = 5.0),
            sample(60.0, 60.0, accuracyM = 50.0),
            sample(50.0, 4.0, accuracyM = 5.0),
        ).filteredPositions(smoothingFactor = 0.5, accuracyThresholdM = 20.0).toList()
        // Le point imprécis n'a pas bougé le lissage : 48 → 50 à 0,5 = 49.
        out shouldContainExactly listOf(LatLng(48.0, 2.0), LatLng(49.0, 3.0))
    }

    "le flux reste stable sur une position immobile" {
        val out = flowOf(sample(48.0, 2.0), sample(48.0, 2.0), sample(48.0, 2.0))
            .filteredPositions(smoothingFactor = 0.3, accuracyThresholdM = 20.0)
            .toList()
        out shouldContainExactly listOf(LatLng(48.0, 2.0), LatLng(48.0, 2.0), LatLng(48.0, 2.0))
    }

    "le flux rejette un seuil de précision non strictement positif" {
        shouldThrow<IllegalArgumentException> {
            flowOf(sample(48.0, 2.0)).filteredPositions(smoothingFactor = 0.5, accuracyThresholdM = 0.0)
        }
    }
})
