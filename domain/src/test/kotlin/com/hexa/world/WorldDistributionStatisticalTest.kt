package com.hexa.world

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/** Grille lat/lng à pas irrégulier couvrant le globe, dédupliquée en cellules H3 distinctes. */
private fun H3CellCenter.globalSample(): List<Long> = buildList {
    var lat = -88.0
    while (lat <= 88.0) {
        var lng = -180.0
        while (lng < 180.0) {
            add(cellAt(lat, lng))
            lng += 1.63
        }
        lat += 1.63
    }
}.distinct()

/**
 * Mesure empirique sur grand échantillon (PRD #3, user story 12 ; issue #16).
 *
 * Les seuils de [com.hexa.config.GameConfig] ont été **recalés** sur cette mesure : la présence
 * mesurée approche désormais les cibles du game design, ce qu'on vérifie à **tolérances larges**.
 * La génération étant strictement déterministe, ces mesures sont reproductibles à l'identique sur
 * toute machine — aucune flakiness. Le rapport lisible est produit par la dernière assertion (et par
 * la tâche `./gradlew :domain:worldDistributionReport`).
 */
class WorldDistributionStatisticalTest : StringSpec({
    val locator = H3CellCenter()
    val generator = WorldGenerator(locator)
    val cells = locator.globalSample()
    val stats = WorldDistribution.measure(cells.map { generator.contentOf(it) })

    "l'échantillon couvre au moins 10 000 cellules distinctes réparties sur le globe" {
        cells.size shouldBeGreaterThanOrEqual 10_000
    }

    "la distribution respecte les invariants structurels : présence décroissante avec la rareté, bornes valides" {
        val rates = Element.entries.map { stats.presenceRate(it) }
        rates.zipWithNext().forEach { (commoner, rarer) -> commoner shouldBeGreaterThanOrEqualTo rarer }
        rates.forEach {
            it shouldBeGreaterThanOrEqualTo 0.0
            it shouldBeLessThanOrEqualTo 1.0
        }
        stats.emptyRate shouldBeGreaterThanOrEqualTo 0.0
        stats.emptyRate shouldBeLessThanOrEqualTo 1.0
    }

    "la présence, la moyenne et les tuiles vides mesurées approchent les cibles (tolérances larges)" {
        Element.entries.forEach { element ->
            val target = DistributionTargets.presenceByElement.getValue(element)
            stats.presenceRate(element) shouldBe (target plusOrMinus 0.03)
        }
        stats.averageElementsPerTile shouldBe (DistributionTargets.AVERAGE_ELEMENTS_PER_TILE plusOrMinus 0.2)
        stats.emptyRate shouldBe (DistributionTargets.EMPTY_RATE plusOrMinus 0.05)
    }

    "le rapport mesuré vs cibles est produit (commande documentée)" {
        println(WorldDistributionReport.report(stats))
    }
})
