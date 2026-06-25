package com.hexa.ui.theme

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Verrouille les tokens de couleur d'élément : cinq couleurs **distinctes** et **opaques**.
 *
 * Pendant de [com.hexa.inventory.labelOf] côté libellés : ce test attrape les copier-coller (deux
 * éléments partageant la même teinte) et les alphas oubliés, ces tokens étant consommés via le
 * registre [ObjectAssets] (couleur d'identité des éléments).
 */
class HexaElementColorsTest : StringSpec({
    "les cinq couleurs d'élément sont distinctes" {
        HexaElementColors.all.toSet() shouldHaveSize 5
    }

    "chaque couleur d'élément est entièrement opaque" {
        HexaElementColors.all.forEach { it.alpha shouldBe 1f }
    }
})
