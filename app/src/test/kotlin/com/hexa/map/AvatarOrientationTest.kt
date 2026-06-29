package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Verrouille la **fonction pure d'orientation** de l'avatar : `lacet = (calage + cap) mod 360`.
 * C'est la frontière purement mathématique de l'orientation boussole (#100), vérifiable sans Mapbox
 * ni capteur — on prouve ici le contrat (calage préservé à cap nul, rotation égale du cap et du
 * lacet, normalisation circulaire, composition franchissant le tour). Le rendu Mapbox et le **signe**
 * effectif de la rotation restent validés à l'œil sur device.
 */
class AvatarOrientationTest : StringSpec({
    val facing = 90.0
    val tol = 1e-9

    "à cap nul, le lacet du modèle vaut le calage du mesh (calage préservé, jamais écrasé)" {
        avatarModelYawDeg(headingDeg = 0.0, facingOffsetDeg = facing) shouldBe (facing plusOrMinus tol)
    }

    "tourner le cap d'un angle tourne le lacet du même angle (composition additive)" {
        val base = avatarModelYawDeg(headingDeg = 30.0, facingOffsetDeg = facing)
        val turned = avatarModelYawDeg(headingDeg = 70.0, facingOffsetDeg = facing)
        (turned - base) shouldBe (40.0 plusOrMinus tol)
    }

    "le lacet est toujours normalisé dans [0, 360)" {
        listOf(-720.0, -45.0, 0.0, 200.0, 359.9, 360.0, 1000.0).forEach { heading ->
            val yaw = avatarModelYawDeg(headingDeg = heading, facingOffsetDeg = facing)
            (yaw >= 0.0 && yaw < 360.0) shouldBe true
        }
    }

    "la composition calage + cap qui franchit un tour reste normalisée" {
        // 90 + 300 = 390 → 30
        avatarModelYawDeg(headingDeg = 300.0, facingOffsetDeg = facing) shouldBe (30.0 plusOrMinus tol)
    }

    "un cap brut hors plage est ramené dans le tour (cap équivalent → même lacet)" {
        avatarModelYawDeg(headingDeg = -90.0, facingOffsetDeg = facing) shouldBe
            (avatarModelYawDeg(headingDeg = 270.0, facingOffsetDeg = facing) plusOrMinus tol)
    }
})
