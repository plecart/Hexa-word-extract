package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Logique **pure** du fondu de la grille avec l'éloignement au joueur (#127) : distance en anneaux →
 * facteur d'opacité. Testée seule, sans device ni Mapbox. On vérifie le **contrat** (centre plein,
 * décroissance monotone, quasi nul au bord, bornes défensives) plutôt que des valeurs en dur, pour
 * rester verte si la courbe est affinée à la validation DA.
 */
class GridFadeTest : StringSpec({
    "la tuile du joueur (distance 0) est pleinement opaque" {
        GridFade.factorFor(0) shouldBe 1f
    }

    "le facteur décroît strictement avec la distance en anneaux, jusqu'au bord du rayon rendu" {
        val factors = (0..MapConfig.GRID_RENDER_RINGS).map { GridFade.factorFor(it) }
        factors.zipWithNext().forAll { (near, far) -> far shouldBeLessThan near }
    }

    "au bord du rayon rendu, le facteur est quasi nul : les tuiles lointaines s'effacent" {
        val edge = GridFade.factorFor(MapConfig.GRID_RENDER_RINGS)
        edge shouldBeGreaterThan 0f // encore dessinée, pas retirée…
        edge shouldBeLessThan 0.2f // …mais quasi effacée
    }

    "le facteur reste borné à [0, 1], même pour une distance dégénérée" {
        GridFade.factorFor(-1) shouldBe 1f // distance négative (cas H3 dégénéré) → borné haut
        GridFade.factorFor(1_000) shouldBe 0f // très au-delà du rayon → borné bas
    }
})
