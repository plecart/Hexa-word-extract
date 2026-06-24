package com.hexa.player

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Cœur **pur** des règles de placement d'un bâtiment (cf. PRD #4, user stories 5-7). Chaque condition
 * — tuile courante, tuile libre, stock disponible — et chaque raison de refus distincte sont vérifiées
 * isolément, sans aucune I/O : trois booléens/entiers en entrée, une [PlacementDecision] en sortie.
 *
 * L'ordre d'évaluation est **verrouillé** (tuile courante → occupation → stock) : quand plusieurs
 * conditions échouent, c'est la première dans cet ordre qui motive le refus, pour un message stable.
 */
class PlacementRulesTest : StringSpec({
    "autorise la pose : tuile courante, libre et stock positif" {
        PlacementRules.decide(isCurrentTile = true, isTileOccupied = false, stock = 1) shouldBe
            PlacementDecision.Placeable
    }

    "refuse hors de la tuile courante" {
        PlacementRules.decide(isCurrentTile = false, isTileOccupied = false, stock = 1) shouldBe
            PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)
    }

    "refuse sur une tuile déjà bâtie" {
        PlacementRules.decide(isCurrentTile = true, isTileOccupied = true, stock = 1) shouldBe
            PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)
    }

    "refuse quand le stock est vide" {
        PlacementRules.decide(isCurrentTile = true, isTileOccupied = false, stock = 0) shouldBe
            PlacementDecision.Refused(PlacementRefusal.NO_STOCK)
    }

    "ordre d'évaluation : hors tuile courante prime sur occupation et stock" {
        PlacementRules.decide(isCurrentTile = false, isTileOccupied = true, stock = 0) shouldBe
            PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)
    }

    "ordre d'évaluation : occupation prime sur stock vide" {
        PlacementRules.decide(isCurrentTile = true, isTileOccupied = true, stock = 0) shouldBe
            PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)
    }
})
