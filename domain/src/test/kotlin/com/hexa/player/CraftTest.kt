package com.hexa.player

import com.hexa.config.Element
import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

/**
 * Module **pur** de craft (cf. PRD #5, user stories 8-11) : recette + état joueur → succès (inventaire
 * débité, stock de bâtiments crédité) ou refus motivé (éléments manquants). Sans I/O ni horloge : un
 * `Player` en entrée, un verdict en sortie. La recette est lue depuis [GameConfig], point unique
 * d'équilibrage.
 */
class CraftTest : StringSpec({
    val createdAt = Instant.parse("2026-06-14T10:15:30Z")

    "craft réussi : débite l'inventaire de la recette et crédite +1 le stock de bâtiments" {
        val player = Player.newPlayer(createdAt) // kit de départ : 250 Cendrite, 100 Givrelin

        val outcome = Craft.build(player, BuildingType.EXTRACTEUR)

        val built = outcome.shouldBeInstanceOf<CraftOutcome.Built>()
        built.player.inventory[Element.CENDRITE] shouldBe 150L // 250 - 100
        built.player.inventory[Element.GIVRELIN] shouldBe 60L // 100 - 40
        built.player.builtBuildings.getValue(BuildingType.EXTRACTEUR) shouldBe 1
    }

    "craft réussi : ne touche que les ressources de la recette et n'altère pas le reste du document" {
        val player = Player.newPlayer(createdAt)

        val built = Craft.build(player, BuildingType.EXTRACTEUR).shouldBeInstanceOf<CraftOutcome.Built>()

        built.player.inventory[Element.LITHOSEVE] shouldBe player.inventory[Element.LITHOSEVE]
        built.player.createdAt shouldBe player.createdAt
        built.player.baseCell shouldBe player.baseCell
    }

    "craft refusé : ressources insuffisantes → refus motivé par les manquants, état joueur inchangé" {
        val player = Player.newPlayer(createdAt)
            .copy(inventory = Inventory.of(mapOf(Element.CENDRITE to 30, Element.GIVRELIN to 40)))

        val outcome = Craft.build(player, BuildingType.EXTRACTEUR)

        val refused = outcome.shouldBeInstanceOf<CraftOutcome.Refused>()
        refused.missing shouldBe mapOf(Element.CENDRITE to 70L) // 100 - 30 ; Givrelin pile suffisant
    }

    "craft à ressources exactes : réussit et vide les compteurs de la recette à zéro" {
        val player = Player.newPlayer(createdAt)
            .copy(inventory = Inventory.of(GameConfig.RECIPE_EXTRACTEUR))

        val built = Craft.build(player, BuildingType.EXTRACTEUR).shouldBeInstanceOf<CraftOutcome.Built>()

        built.player.inventory[Element.CENDRITE] shouldBe 0L
        built.player.inventory[Element.GIVRELIN] shouldBe 0L
    }
})
