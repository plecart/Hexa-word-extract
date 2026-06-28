package com.hexa.map

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [CameraRotationGesture] est la logique **pure** « bearing piloté par le glisser » : le doigt qui
 * tourne autour du joueur (centre écran) emmène la carte, qui pivote du même angle (1:1). On couvre
 * la mesure d'angle ([CameraRotationGesture.angleDeg], repère écran y vers le bas) et le report sur le
 * cap ([CameraRotationGesture.rotate], avec wrap circulaire) — sans device ni Compose. C'est le
 * critère « logique pure (bearing piloté par le geste) » de l'issue.
 */
class CameraRotationGestureTest : StringSpec({
    "angleDeg : un point à droite du centre est à 0°" {
        CameraRotationGesture.angleDeg(centerX = 100f, centerY = 100f, x = 200f, y = 100f) shouldBe 0.0
    }

    "angleDeg : un point sous le centre est à +90° (repère écran, y vers le bas)" {
        CameraRotationGesture.angleDeg(centerX = 100f, centerY = 100f, x = 100f, y = 200f) shouldBe 90.0
    }

    "angleDeg : un point au-dessus du centre est à -90°" {
        CameraRotationGesture.angleDeg(centerX = 100f, centerY = 100f, x = 100f, y = 0f) shouldBe -90.0
    }

    "rotate : sans rotation du doigt, le cap reste celui de départ" {
        CameraRotationGesture.rotate(startBearingDeg = 50.0, startAngleDeg = 30.0, currentAngleDeg = 30.0) shouldBe 50.0
    }

    "rotate : le doigt qui tourne dans le sens horaire à l'écran fait suivre la carte (le cap diminue)" {
        CameraRotationGesture.rotate(startBearingDeg = 100.0, startAngleDeg = 0.0, currentAngleDeg = 30.0) shouldBe 70.0
    }

    "rotate : le cap est normalisé au franchissement de 0° (wrap circulaire vers le bas)" {
        CameraRotationGesture.rotate(startBearingDeg = 10.0, startAngleDeg = 0.0, currentAngleDeg = 30.0) shouldBe 340.0
    }

    "rotate : le cap est normalisé au franchissement de 360° (wrap circulaire vers le haut)" {
        CameraRotationGesture.rotate(startBearingDeg = 350.0, startAngleDeg = 30.0, currentAngleDeg = 0.0) shouldBe 20.0
    }
})
