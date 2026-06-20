package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [DemoBuiltTiles] alimente l'état « bâtie » avec des cellules **factices** en attendant les vraies
 * données de bâtiments. On vérifie qu'il est déterministe (même cellule → même verdict) et qu'il
 * marque une fraction non triviale d'un échantillon, pour que les trois états soient démontrables.
 */
class DemoBuiltTilesTest : StringSpec({
    "le verdict est déterministe pour une cellule donnée" {
        val cell = 612_345_678_901_234_567L
        DemoBuiltTiles.contains(cell) shouldBe DemoBuiltTiles.contains(cell)
    }

    "marque une fraction non triviale d'un échantillon de cellules" {
        // Sur un échantillon, certaines tuiles sont bâties et d'autres non : les deux états cohabitent.
        val sample = (1L..200L).map { it * 1_000_003L }
        val built = sample.count(DemoBuiltTiles::contains)
        built shouldNotBe 0
        built shouldNotBe sample.size
    }
})
