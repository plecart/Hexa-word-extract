package com.hexa.ui.theme

import androidx.compose.animation.core.AnimationVector1D
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Verrouille le converteur `Long ↔ AnimationVector1D` ([LongToVector]) qui rend une quantité
 * d'inventaire animable (Compose ne fournit de converteur que pour `Int`). Deux garanties :
 * l'aller-retour **préserve** la valeur sur la plage réaliste des stocks, et la lecture depuis le
 * vecteur **arrondit** au plus proche (le compteur affiche un entier net en cours d'animation).
 */
class LongVectorConverterTest : StringSpec({
    "le converteur préserve la quantité sur un aller-retour" {
        listOf(0L, 5L, 87L, 1_240L, 999_999L).forEach { amount ->
            LongToVector.convertFromVector(LongToVector.convertToVector(amount)) shouldBe amount
        }
    }

    "la lecture depuis le vecteur arrondit à l'entier le plus proche" {
        LongToVector.convertFromVector(AnimationVector1D(6.4f)) shouldBe 6L
        LongToVector.convertFromVector(AnimationVector1D(6.6f)) shouldBe 7L
    }
})
