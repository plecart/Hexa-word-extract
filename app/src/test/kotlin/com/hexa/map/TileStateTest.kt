package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Classification **pure** d'une cellule en l'un des trois états visuels de la grille. On vérifie la
 * priorité (la tuile courante prime sur « bâtie ») et le repli sur « normale », sans device ni H3.
 */
class TileStateTest : StringSpec({
    val current = 100L
    val built = BuiltTiles { it == 200L || it == 201L }

    "la cellule sous le joueur est COURANTE" {
        tileState(cell = 100L, current = current, builtTiles = built) shouldBe TileState.COURANTE
    }

    "la tuile courante prime sur l'état bâti" {
        val builtCurrent = BuiltTiles { it == 100L }
        tileState(cell = 100L, current = current, builtTiles = builtCurrent) shouldBe TileState.COURANTE
    }

    "une cellule bâtie non courante est BATIE" {
        tileState(cell = 200L, current = current, builtTiles = built) shouldBe TileState.BATIE
    }

    "une cellule ni courante ni bâtie est NORMALE" {
        tileState(cell = 999L, current = current, builtTiles = built) shouldBe TileState.NORMALE
    }
})
