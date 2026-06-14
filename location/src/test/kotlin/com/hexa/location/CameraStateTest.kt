package com.hexa.location

import com.hexa.core.geo.LatLng
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [CameraState] est un type valeur : il décrit une pose de caméra (centre, zoom, pitch, cap) sans
 * logique. On vérifie le contrat d'un type valeur — accès aux composantes et égalité structurelle ;
 * le calcul de cette pose est testé dans [ChaseCameraControllerTest].
 */
class CameraStateTest : StringSpec({
    "expose le centre, le zoom, le pitch et le cap fournis" {
        val state = CameraState(
            center = LatLng(48.85, 2.35),
            zoomLevel = 17.5,
            pitchDeg = 60.0,
            bearingDeg = 90.0,
        )
        state.center shouldBe LatLng(48.85, 2.35)
        state.zoomLevel shouldBe 17.5
        state.pitchDeg shouldBe 60.0
        state.bearingDeg shouldBe 90.0
    }

    "deux poses de mêmes composantes sont égales" {
        CameraState(LatLng(48.85, 2.35), 17.5, 60.0, 90.0) shouldBe
            CameraState(LatLng(48.85, 2.35), 17.5, 60.0, 90.0)
    }
})
