package com.hexa.map

import com.hexa.player.PlacedBuilding
import com.hexa.player.PlacementDecision
import com.hexa.player.PlacementRefusal
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Glu pure entre l'état observable de la carte et les règles de placement (`PlacementRules`, couvert
 * par `PlacementRulesTest`) : décide la tuile où afficher le marqueur « + », ou `null`. Vérifie le
 * branchement des trois conditions sur les données réelles de la carte (tuile courante résolue en H3,
 * occupation lue dans les bâtiments posés, stock).
 */
class ExtractorPlacementTest : StringSpec({
    val now = Instant.parse("2026-06-22T08:30:00Z")
    val toH3String: (Long) -> String = { "h3-$it" }
    val currentCell = 100L

    "propose la tuile courante quand elle est libre et le stock positif" {
        extractorPlacementCell(
            currentTile = currentCell,
            placedBuildings = emptyList(),
            extractorStock = 1,
            toH3String = toH3String,
        ) shouldBe "h3-100"
    }

    "ne propose rien sans tuile courante" {
        extractorPlacementCell(
            currentTile = null,
            placedBuildings = emptyList(),
            extractorStock = 1,
            toH3String = toH3String,
        ) shouldBe null
    }

    "ne propose rien sur une tuile courante déjà bâtie" {
        extractorPlacementCell(
            currentTile = currentCell,
            placedBuildings = listOf(PlacedBuilding.base("h3-100", now)),
            extractorStock = 1,
            toH3String = toH3String,
        ) shouldBe null
    }

    "ne propose rien quand le stock est vide" {
        extractorPlacementCell(
            currentTile = currentCell,
            placedBuildings = emptyList(),
            extractorStock = 0,
            toH3String = toH3String,
        ) shouldBe null
    }

    // placementDecisionFor : cœur de glu partagé entre le marqueur « + » et l'inspection. On y vérifie
    // ce qu'il ajoute à PlacementRules.decide (couvert par PlacementRulesTest) : la dérivation de
    // l'occupation depuis les bâtiments posés, et le passage des trois conditions à la décision — un cas
    // par issue possible de PlacementDecision.
    "placementDecisionFor refuse hors de la tuile courante" {
        placementDecisionFor(
            isCurrent = false,
            cell = "h3-100",
            placedBuildings = emptyList(),
            stock = 1,
        ) shouldBe PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)
    }

    "placementDecisionFor lit l'occupation dans les bâtiments posés sur la cellule" {
        placementDecisionFor(
            isCurrent = true,
            cell = "h3-100",
            placedBuildings = listOf(PlacedBuilding.base("h3-100", now)),
            stock = 1,
        ) shouldBe PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)
    }

    "placementDecisionFor ignore les bâtiments posés sur d'autres cellules" {
        placementDecisionFor(
            isCurrent = true,
            cell = "h3-100",
            placedBuildings = listOf(PlacedBuilding.base("h3-999", now)),
            stock = 1,
        ) shouldBe PlacementDecision.Placeable
    }

    "placementDecisionFor refuse quand le stock est vide" {
        placementDecisionFor(
            isCurrent = true,
            cell = "h3-100",
            placedBuildings = emptyList(),
            stock = 0,
        ) shouldBe PlacementDecision.Refused(PlacementRefusal.NO_STOCK)
    }
})
