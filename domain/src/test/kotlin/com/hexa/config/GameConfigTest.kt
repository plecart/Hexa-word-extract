package com.hexa.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Verrouille les constantes du game design (document d'équilibrage §6) contre toute
 * dérive accidentelle. Ces valeurs sont provisoires mais doivent rester cohérentes :
 * les listes indexées par rareté ont toutes la même longueur que [Element.entries].
 */
class GameConfigTest : StringSpec({
    "la résolution H3 est 11" {
        GameConfig.H3_RESOLUTION shouldBe 11
    }

    "les listes indexées par rareté couvrent les cinq éléments" {
        val rarityCount = Element.entries.size
        GameConfig.WAVELENGTHS_M shouldHaveSize rarityCount
        GameConfig.PRESENCE_THRESHOLDS shouldHaveSize rarityCount
        GameConfig.BASE_RATES_PER_HOUR shouldHaveSize rarityCount
    }

    "les seuils de présence et les longueurs d'onde suivent les valeurs d'équilibrage" {
        GameConfig.WAVELENGTHS_M shouldBe listOf(2000, 1500, 1000, 600, 400)
        GameConfig.PRESENCE_THRESHOLDS shouldBe listOf(0.45, 0.70, 0.85, 0.93, 0.97)
        GameConfig.BASE_RATES_PER_HOUR shouldBe listOf(60, 30, 14, 6, 2)
    }

    "les constantes de bruit et de récolte sont fixées" {
        GameConfig.RATE_FLOOR shouldBe 0.20
        GameConfig.OCTAVE2_AMPLITUDE shouldBe 0.25
        GameConfig.OCTAVE2_FREQ_MULT shouldBe 4
        GameConfig.COLLECT_REFRESH_SECONDS shouldBe 30
    }

    "la recette d'extracteur et le kit de départ sont libellés par élément" {
        GameConfig.RECIPE_EXTRACTEUR shouldContainExactly
            mapOf(Element.CENDRITE to 100, Element.GIVRELIN to 40)
        GameConfig.STARTER_KIT shouldContainExactly
            mapOf(Element.CENDRITE to 250, Element.GIVRELIN to 100)
    }
})
