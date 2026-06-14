package com.hexa.location

import com.hexa.core.geo.LatLng
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * [ChaseCameraController] est la **politique pure** de poursuite : il décide, selon le mode, la pose
 * de caméra à appliquer (ou de rendre la main à l'utilisateur). On couvre les quatre comportements
 * attendus par l'issue — poursuite, mode libre après geste, recentrage, bornes de zoom — sans
 * aucune dépendance Mapbox.
 */
class ChaseCameraControllerTest : StringSpec({
    val config = ChaseCameraConfig(pitchDeg = 60.0, followZoom = 17.0, minZoom = 14.0, maxZoom = 19.0)
    val paris = LatLng(48.8566, 2.3522)

    "démarre en mode poursuite" {
        ChaseCameraController(config).mode shouldBe CameraMode.FOLLOW
    }

    "en poursuite sans zoom utilisateur, centre sur la position au pitch, cap et zoom configurés" {
        val state = ChaseCameraController(config).cameraFor(position = paris, headingDeg = 90.0)
        state shouldBe CameraState(center = paris, zoomLevel = 17.0, pitchDeg = 60.0, bearingDeg = 90.0)
    }

    "en poursuite, un zoom utilisateur dans les bornes est respecté" {
        val state = ChaseCameraController(config).cameraFor(paris, headingDeg = 0.0, userZoom = 16.0)
        state?.zoomLevel shouldBe 16.0
    }

    "en poursuite, un zoom utilisateur au-delà du maximum est borné" {
        val state = ChaseCameraController(config).cameraFor(paris, headingDeg = 0.0, userZoom = 25.0)
        state?.zoomLevel shouldBe 19.0
    }

    "en poursuite, un zoom utilisateur en-deçà du minimum est borné" {
        val state = ChaseCameraController(config).cameraFor(paris, headingDeg = 0.0, userZoom = 5.0)
        state?.zoomLevel shouldBe 14.0
    }

    "après un geste manuel, passe en mode libre et rend la main (aucune pose imposée)" {
        val released = ChaseCameraController(config).releasedByGesture()
        released.mode shouldBe CameraMode.FREE
        released.cameraFor(paris, headingDeg = 0.0).shouldBeNull()
    }

    "le recentrage réengage la poursuite après un mode libre" {
        val recentered = ChaseCameraController(config).releasedByGesture().recentered()
        recentered.mode shouldBe CameraMode.FOLLOW
        recentered.cameraFor(paris, headingDeg = 45.0)?.bearingDeg shouldBe 45.0
    }

    "rejette une configuration aux bornes de zoom incohérentes" {
        shouldThrow<IllegalArgumentException> {
            ChaseCameraConfig(pitchDeg = 60.0, followZoom = 17.0, minZoom = 19.0, maxZoom = 14.0)
        }
    }

    "rejette une configuration dont le zoom de poursuite sort des bornes" {
        shouldThrow<IllegalArgumentException> {
            ChaseCameraConfig(pitchDeg = 60.0, followZoom = 25.0, minZoom = 14.0, maxZoom = 19.0)
        }
    }
})
