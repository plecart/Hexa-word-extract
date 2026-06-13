package com.hexa.world

import com.hexa.config.Element
import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

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

    "measure collecte les richesses observées par élément" {
        val stats = WorldDistribution.measure(
            listOf(
                tile(deposit(Element.CENDRITE, richness = 0.3)),
                tile(deposit(Element.CENDRITE, richness = 0.7), deposit(Element.GIVRELIN, richness = 0.4)),
            ),
        )
        stats.richnessByElement.getValue(Element.CENDRITE) shouldContainExactlyInAnyOrder listOf(0.3, 0.7)
        stats.richnessByElement.getValue(Element.GIVRELIN) shouldContainExactlyInAnyOrder listOf(0.4)
    }

    "proposeThresholds reconstruit les seuils visant les cibles à partir des richesses de référence" {
        // Référence Cendrite (seuil 0,45) : 6 tuiles présentes sur 10 → taux de présence 0,6.
        // Les richesses choisies reconstruisent v = [0,5 .. 1,0], dont les quantiles sont vérifiables.
        val refThreshold = GameConfig.PRESENCE_THRESHOLDS[Element.CENDRITE.ordinal]
        val reconstructedV = listOf(0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
        val richness = reconstructedV.map { (it - refThreshold) / (1.0 - refThreshold) }
        val stats = DistributionStats(
            totalTiles = 10,
            presenceCount = Element.entries.associateWith { 0 } + mapOf(Element.CENDRITE to 6),
            emptyTiles = 4,
            richnessByElement = Element.entries.associateWith { emptyList<Double>() } +
                mapOf(Element.CENDRITE to richness),
        )

        val proposed = WorldDistribution.proposeThresholds(
            stats,
            presenceTargets = mapOf(Element.GIVRELIN to 0.6, Element.LITHOSEVE to 0.3),
        )

        // cible 0,6 → quantile plein 0,4 = borne basse des présents = 0,50
        proposed.getValue(Element.GIVRELIN) shouldBe (0.50 plusOrMinus 1e-9)
        // cible 0,3 → quantile plein 0,7 = médiane des présents = 0,75
        proposed.getValue(Element.LITHOSEVE) shouldBe (0.75 plusOrMinus 1e-9)
    }
})
