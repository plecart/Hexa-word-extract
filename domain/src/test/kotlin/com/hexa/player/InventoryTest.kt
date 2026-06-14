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
})
