package com.hexa.core.geo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * [wrapDegrees] ramène un angle en degrés dans `[0, 360)`. C'est l'unique point de wrap d'angle du
 * projet (consommé par le lissage de cap et la source boussole) : on couvre les valeurs déjà dans
 * l'intervalle, négatives, au-delà d'un tour, et les bornes exactes (360° → 0°).
 */
class AnglesTest : StringSpec({
    val eps = 1e-9

    "laisse inchangé un angle déjà dans [0, 360)" {
        10.0.wrapDegrees() shouldBe (10.0 plusOrMinus eps)
    }

    "ramène un angle négatif dans [0, 360)" {
        (-10.0).wrapDegrees() shouldBe (350.0 plusOrMinus eps)
    }

    "ramène un angle supérieur à 360° dans [0, 360)" {
        370.0.wrapDegrees() shouldBe (10.0 plusOrMinus eps)
    }

    "ramène un tour complet (360°) sur 0°" {
        360.0.wrapDegrees() shouldBe (0.0 plusOrMinus eps)
    }

    "ramène un angle négatif de plus d'un tour dans [0, 360)" {
        (-370.0).wrapDegrees() shouldBe (350.0 plusOrMinus eps)
    }
})
