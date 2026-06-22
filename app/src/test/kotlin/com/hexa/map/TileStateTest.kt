package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Classification **pure** d'une cellule en son état visuel : la tuile sous le joueur est courante,
 * toutes les autres sont normales. Sans device ni H3.
 */
class TileStateTest : StringSpec({
    val current = 100L

    "la cellule sous le joueur est COURANTE" {
        tileState(cell = 100L, current = current) shouldBe TileState.COURANTE
    }

    "une cellule qui n'est pas la tuile courante est NORMALE" {
        tileState(cell = 999L, current = current) shouldBe TileState.NORMALE
    }
})
