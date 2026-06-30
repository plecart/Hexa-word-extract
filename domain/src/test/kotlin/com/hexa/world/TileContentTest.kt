package com.hexa.world

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Sélection **pure** de l'élément le plus rare d'une tuile : socle de la recoloration des hexagones
 * (#126), où la teinte d'une tuile vient de l'identité de son élément le plus rare. La rareté suit
 * l'ordre déclaré de [Element] (croissante), indépendamment de l'ordre des gisements dans la liste.
 */
class TileContentTest : StringSpec({
    "une tuile à plusieurs gisements retient le plus rare" {
        tile(deposit(Element.CENDRITE), deposit(Element.ECHOFER), deposit(Element.GIVRELIN))
            .rarestElement shouldBe Element.ECHOFER
    }

    "le plus rare ne dépend pas de l'ordre des gisements" {
        tile(deposit(Element.NYCTITE), deposit(Element.CENDRITE)).rarestElement shouldBe Element.NYCTITE
    }

    "une tuile à un seul gisement retient cet élément" {
        tile(deposit(Element.LITHOSEVE)).rarestElement shouldBe Element.LITHOSEVE
    }

    "une tuile sans gisement n'a pas d'élément le plus rare" {
        tile().rarestElement shouldBe null
    }
})
