package com.hexa.world

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

/**
 * Vérifie le **contenu sémantique** du rapport (présence de chaque élément, cible, mesure, section
 * de seuils proposés) — pas sa mise en forme exacte, qui peut évoluer sans casser le sens.
 */
class WorldDistributionReportTest : StringSpec({
    "le rapport montre cible et mesure par élément, plus la section des seuils proposés" {
        // Cendrite présent dans 1 tuile sur 2 → 50,0 % mesuré ; cible 55,0 %.
        val stats = WorldDistribution.measure(
            listOf(
                tile(deposit(Element.CENDRITE, richness = 0.5)),
                tile(),
            ),
        )

        val report = WorldDistributionReport.report(stats)

        Element.entries.forEach { element ->
            report shouldContain element.name.lowercase().replaceFirstChar { it.uppercase() }
        }
        report shouldContain "55.0" // cible Cendrite, point décimal (Locale.ROOT) quelle que soit la machine
        report shouldContain "50.0" // mesuré Cendrite
        report shouldContain "Seuils proposés"
    }
})
