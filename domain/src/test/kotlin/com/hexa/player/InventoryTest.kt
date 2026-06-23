package com.hexa.player

import com.hexa.config.Element
import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Vérifie l'invariant de l'inventaire : les cinq [Element] sont **toujours** présents avec un
 * compteur, et la construction depuis une carte partielle (le kit de départ) complète les éléments
 * absents à zéro.
 */
class InventoryTest : StringSpec({
    "l'inventaire vide expose les cinq compteurs à zéro" {
        Element.entries.forEach { element ->
            Inventory.EMPTY[element] shouldBe 0L
        }
    }

    "construire depuis le kit de départ crédite les éléments fournis et laisse les autres à zéro" {
        val inventory = Inventory.of(GameConfig.STARTER_KIT)

        inventory[Element.CENDRITE] shouldBe 250L
        inventory[Element.GIVRELIN] shouldBe 100L
        inventory[Element.LITHOSEVE] shouldBe 0L
        inventory[Element.ECHOFER] shouldBe 0L
        inventory[Element.NYCTITE] shouldBe 0L
    }

    "créditer ajoute les gains aux éléments concernés et laisse les autres intacts" {
        val inventory = Inventory.of(mapOf(Element.CENDRITE to 250, Element.NYCTITE to 1))

        val credited = inventory.plus(mapOf(Element.CENDRITE to 40L, Element.LITHOSEVE to 7L))

        credited[Element.CENDRITE] shouldBe 290L // 250 + 40
        credited[Element.LITHOSEVE] shouldBe 7L // 0 + 7
        credited[Element.NYCTITE] shouldBe 1L // intact, absent des gains
        credited[Element.GIVRELIN] shouldBe 0L // intact
    }

    "créditer des gains vides laisse l'inventaire inchangé" {
        val inventory = Inventory.of(GameConfig.STARTER_KIT)

        inventory.plus(emptyMap()) shouldBe inventory
    }
})
