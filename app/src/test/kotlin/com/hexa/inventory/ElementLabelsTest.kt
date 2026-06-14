package com.hexa.inventory

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe

/**
 * Verrouille la correspondance élément → libellé : un libellé **distinct** pour chacun des cinq
 * éléments. L'exhaustivité est garantie par le compilateur (`when` sans `else`) ; ce test attrape les
 * doublons de ressource (copier-coller) et les libellés manquants (id à zéro).
 */
class ElementLabelsTest : StringSpec({
    "chaque élément a un libellé de ressource distinct et non nul" {
        val ids = Element.entries.map(::labelOf)

        ids.toSet() shouldHaveSize Element.entries.size
        ids.forEach { it shouldNotBe 0 }
    }
})
