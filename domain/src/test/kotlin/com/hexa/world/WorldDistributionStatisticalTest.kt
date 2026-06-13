package com.hexa.world

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual

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
 * Avec les seuils **provisoires**, la distribution ne colle pas aux cibles (c'est l'objet même de
 * cette mesure). On n'asserte donc **pas** la proximité aux cibles — cela rendrait le test rouge et
 * forcerait un recalage silencieux des constantes. On vérifie les invariants **indépendants du
 * calage** ; l'écart aux cibles et les seuils recalés sont **rapportés** au mainteneur.
 */
class WorldDistributionStatisticalTest : StringSpec({
    val locator = H3CellCenter()
    val generator = WorldGenerator(locator)
    val cells = locator.globalSample()

    "l'échantillon couvre au moins 10 000 cellules distinctes réparties sur le globe" {
        cells.size shouldBeGreaterThanOrEqual 10_000
    }

    "la distribution mesurée respecte les invariants structurels ; le rapport est produit" {
        val stats = WorldDistribution.measure(cells.map { generator.contentOf(it) })

        // Présence décroissante avec la rareté (seuil plus haut ⇒ moins présent) — toujours vrai.
        val rates = Element.entries.map { stats.presenceRate(it) }
        rates.zipWithNext().forEach { (commoner, rarer) -> commoner shouldBeGreaterThanOrEqualTo rarer }
        rates.forEach {
            it shouldBeGreaterThanOrEqualTo 0.0
            it shouldBeLessThanOrEqualTo 1.0
        }
        stats.emptyRate shouldBeGreaterThanOrEqualTo 0.0
        stats.emptyRate shouldBeLessThanOrEqualTo 1.0
        stats.presenceRate(Element.CENDRITE) shouldBeGreaterThan 0.3 // non-vacuité de la mesure

        // Sortie du rapport mesuré vs cibles (la « commande documentée » — cf. tâche Gradle).
        println(WorldDistributionReport.report(stats))
    }
})
