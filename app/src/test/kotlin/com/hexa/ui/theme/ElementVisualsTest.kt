package com.hexa.ui.theme

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Verrouille le mapping `Element → identité visuelle` ([ElementVisuals]) : une couleur **distincte**
 * pour chacun des cinq éléments. L'exhaustivité (« pas d'absent ») est garantie par le compilateur
 * (`when` sans `else`) et exercée ici en parcourant `Element.entries` ; ce test attrape en plus les
 * **doublons** (deux éléments partageant la même teinte).
 */
class ElementVisualsTest : StringSpec({
    val visualColors = Element.entries.map { ElementVisuals.of(it).color }

    "chaque élément a une couleur de visuel distincte" {
        visualColors.toSet() shouldHaveSize Element.entries.size
    }

    "le visuel reprend le token de couleur canonique, dans l'ordre de rareté" {
        visualColors shouldBe HexaElementColors.all
    }
})
