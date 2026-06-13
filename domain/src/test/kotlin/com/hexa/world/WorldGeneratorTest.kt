package com.hexa.world

import com.hexa.config.Element
import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
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

    "les champs des éléments sont indépendants : aucune présence n'en sous-tend une autre" {
        val presence = cells.map { cell -> generator.contentOf(cell).deposits.map { it.element }.toSet() }
        // Aux seuils provisoires, les deux éléments les plus rares ne franchissent jamais le bruit
        // (distribution non uniforme) : leur recalage est explicitement #16. On vérifie donc
        // l'indépendance sur les champs qui se manifestent, garantie qu'au moins les deux communs
        // sont là pour que le test ne passe pas à vide.
        val appearing = Element.entries.filter { element -> presence.any { element in it } }
        appearing shouldContainAll listOf(Element.CENDRITE, Element.GIVRELIN)
        // Si deux champs étaient identiques (offsets non distincts), la présence du seuil le plus
        // haut serait incluse dans celle du plus bas. Exiger les deux sens de non-recouvrement
        // pour chaque couple prouve que les champs ne se sous-tendent pas.
        for (a in appearing) {
            for (b in appearing) {
                if (a != b) {
                    presence.any { a in it && b !in it } shouldBe true
                }
            }
        }
    }

    "les tuiles aux antiméridiens et aux hautes latitudes se génèrent sans erreur, de façon déterministe" {
        val extremes = buildList {
            for (lat in listOf(-89.9, -85.0, 85.0, 89.9)) {
                for (lng in listOf(-180.0, -179.999, 179.999, 180.0)) {
                    add(locator.cellAt(lat, lng))
                }
            }
        }.distinct()
        extremes.forEach { cell ->
            generator.contentOf(cell) shouldBe generator.contentOf(cell)
        }
    }

    "l'antiméridien n'introduit pas de couture : +180° et −180° désignent la même tuile et le même contenu" {
        listOf(-89.0, -45.0, 0.0, 45.0, 89.0).forEach { lat ->
            val east = locator.cellAt(lat, 180.0)
            val west = locator.cellAt(lat, -180.0)
            generator.contentOf(east) shouldBe generator.contentOf(west)
        }
    }
})
