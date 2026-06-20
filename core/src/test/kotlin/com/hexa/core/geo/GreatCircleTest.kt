package com.hexa.core.geo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Vérifie la distance géodésique entre deux positions — fondation de l'hystérésis de tuile
 * courante (comparer la distance du joueur aux centres de deux cellules voisines).
 *
 * On teste le **comportement observable** : nullité sur place, symétrie, ordres de grandeur
 * connus (un degré de latitude, Paris→Londres) — jamais la formule interne.
 */
class GreatCircleTest : StringSpec({
    "deux positions identiques sont à distance nulle" {
        val paris = LatLng(48.8566, 2.3522)
        GreatCircle.distanceMeters(paris, paris) shouldBe (0.0 plusOrMinus 1e-6)
    }

    "un degré de latitude vaut environ 111 km" {
        val a = LatLng(0.0, 0.0)
        val b = LatLng(1.0, 0.0)
        // Un degré de méridien ≈ 111,2 km ; tolérance large car la Terre est modélisée sphérique.
        GreatCircle.distanceMeters(a, b) shouldBe (111_195.0 plusOrMinus 200.0)
    }

    "la distance est symétrique" {
        val a = LatLng(48.8566, 2.3522)
        val b = LatLng(51.5074, -0.1278)
        GreatCircle.distanceMeters(a, b) shouldBe (GreatCircle.distanceMeters(b, a) plusOrMinus 1e-6)
    }

    "Paris–Londres mesure environ 343 km" {
        val paris = LatLng(48.8566, 2.3522)
        val londres = LatLng(51.5074, -0.1278)
        GreatCircle.distanceMeters(paris, londres) shouldBe (343_500.0 plusOrMinus 2_000.0)
    }

    "deux centres de cellules H3 voisines (~25 m) sont à quelques dizaines de mètres" {
        // Deux points distants d'environ 25 m en longitude à Paris (échelle d'une arête res-11).
        val a = LatLng(48.8566, 2.35220)
        val b = LatLng(48.8566, 2.35254)
        GreatCircle.distanceMeters(a, b) shouldBe (25.0 plusOrMinus 5.0)
    }
})
