package com.hexa.inventory

import com.hexa.player.BuildingType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe

/**
 * Verrouille la correspondance bâtiment → libellé : un libellé **distinct** et non nul pour chacun
 * des [BuildingType]. Pendant d'[ElementLabelsTest] côté éléments. L'exhaustivité est garantie par le
 * compilateur (`when` sans `else`) ; ce test attrape les doublons de ressource (copier-coller) et les
 * libellés manquants (id à zéro) — garde-fou scalabilité avant l'ajout d'un 2ᵉ bâtiment.
 */
class BuildingLabelsTest : StringSpec({
    "chaque bâtiment a un libellé de ressource distinct et non nul" {
        val ids = BuildingType.entries.map(::labelOf)

        ids.toSet() shouldHaveSize BuildingType.entries.size
        ids.forEach { it shouldNotBe 0 }
    }
})
