package com.hexa.world

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private fun deposit(element: Element, richness: Double = 0.5, rate: Int = 1) = ElementDeposit(element, richness, rate)

private fun tile(vararg deposits: ElementDeposit) = TileContent(deposits.toList())

/**
 * Vérifie l'agrégateur pur [WorldDistribution.measure] sur des contenus forgés à la main — aucune
 * dépendance au générateur ni à H3. Le grand échantillon réel est l'affaire du test statistique.
 */
class WorldDistributionTest : StringSpec({
    "measure compte les présences par élément, les tuiles totales et les tuiles vides" {
        val stats = WorldDistribution.measure(
            listOf(
                tile(deposit(Element.CENDRITE)),
                tile(),
                tile(deposit(Element.CENDRITE), deposit(Element.GIVRELIN)),
            ),
        )
        stats.totalTiles shouldBe 3
        stats.presenceCount[Element.CENDRITE] shouldBe 2
        stats.presenceCount[Element.GIVRELIN] shouldBe 1
        stats.presenceCount[Element.LITHOSEVE] shouldBe 0
        stats.emptyTiles shouldBe 1
    }

    "measure dérive le taux de présence, le taux de tuiles vides et la moyenne d'éléments par tuile" {
        val stats = WorldDistribution.measure(
            listOf(
                tile(deposit(Element.CENDRITE)),
                tile(),
                tile(deposit(Element.CENDRITE), deposit(Element.GIVRELIN)),
                tile(),
            ),
        )
        stats.presenceRate(Element.CENDRITE) shouldBe (0.5 plusOrMinus 1e-9)
        stats.emptyRate shouldBe (0.5 plusOrMinus 1e-9)
        stats.averageElementsPerTile shouldBe (0.75 plusOrMinus 1e-9)
    }
})
