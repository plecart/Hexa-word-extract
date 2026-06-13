package com.hexa.world

import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe

/**
 * Réseau de cellules H3 variées couvrant le globe — hautes latitudes (±85°) et antiméridien
 * (longitude −180°) inclus. Le pas irrationnel évite tout alignement régulier sur la grille.
 * Plusieurs centaines de cellules distinctes, comme l'exige le critère de déterminisme.
 */
private fun H3CellCenter.variedCells(): List<Long> = buildList {
    var lat = -85.0
    while (lat <= 85.0) {
        var lng = -180.0
        while (lng < 180.0) {
            add(cellAt(lat, lng))
            lng += 11.3
        }
        lat += 7.1
    }
}.distinct()

/**
 * Vérifie le contrat **observable** du générateur de monde (PRD #3) : index H3 → contenu de tuile.
 * On n'inspecte jamais les valeurs internes du bruit — uniquement les éléments présents, leur
 * richesse et leur vitesse, conformément aux décisions de test du PRD.
 */
class WorldGeneratorTest : StringSpec({
    val locator = H3CellCenter()
    val generator = WorldGenerator(locator)
    val cells = locator.variedCells()

    "le contenu d'une tuile est strictement déterministe (même index → contenu identique)" {
        cells.forEach { cell ->
            generator.contentOf(cell) shouldBe generator.contentOf(cell)
        }
    }

    "tout élément présent a une richesse dans ]0, 1] et une vitesse bornée par son taux de base" {
        val deposits = cells.flatMap { generator.contentOf(it).deposits }
        // Non vacuité : Cendrite (seuil 0,45) doit apparaître sur une bonne part des tuiles.
        deposits.shouldNotBeEmpty()
        deposits.forEach { deposit ->
            deposit.richness shouldBeGreaterThan 0.0
            deposit.richness shouldBeLessThanOrEqualTo 1.0
            val base = GameConfig.BASE_RATES_PER_HOUR[deposit.element.ordinal]
            deposit.ratePerHour shouldBeGreaterThanOrEqualTo 0
            deposit.ratePerHour shouldBeLessThanOrEqualTo base
        }
    }
})
