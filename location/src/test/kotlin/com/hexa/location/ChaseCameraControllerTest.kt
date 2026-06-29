package com.hexa.location

import com.hexa.core.geo.LatLng
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [ChaseCameraController] est la **politique pure** de poursuite : la caméra reste verrouillée sur le
 * joueur et il décide la pose à imposer (centre, pitch, cap, zoom borné). On couvre les comportements
 * attendus par l'issue — pose centrée, bornes de zoom et **couplage continu du pitch au zoom** — sans
 * aucune dépendance Mapbox.
 *
 * Config de test : pitch dans `[20°, 70°]` cartographié sur le zoom borné `[14, 19]`, soit un pas de
 * 10°/zoom. Les valeurs sont choisies pour donner des interpolations rondes (cf. chaque test).
 */
class ChaseCameraControllerTest : StringSpec({
    // Config valide de référence ; chaque test ne surcharge que le paramètre qui l'intéresse.
    fun configOf(
        minPitchDeg: Double = 20.0,
        maxPitchDeg: Double = 70.0,
        followZoom: Double = 17.0,
        minZoom: Double = 14.0,
        maxZoom: Double = 19.0,
    ) = ChaseCameraConfig(minPitchDeg, maxPitchDeg, followZoom, minZoom, maxZoom)

    val config = configOf()
    val paris = LatLng(48.8566, 2.3522)

    "sans zoom utilisateur, centre sur la position au cap, zoom et pitch interpolé du zoom de poursuite" {
        // followZoom = 17 → t = (17 − 14) / (19 − 14) = 0,6 → pitch = 20 + 0,6 × 50 = 50°.
        val state = ChaseCameraController(config).cameraFor(position = paris, bearingDeg = 90.0)
        state shouldBe CameraState(center = paris, zoomLevel = 17.0, pitchDeg = 50.0, bearingDeg = 90.0)
    }

    "un zoom au-delà des bornes borne le zoom ET en dérive le pitch (pitch maximal)" {
        // Intégration : cameraFor passe le zoom **borné** (25 → 19) à la courbe → pitch maximal (70°).
        // La courbe elle-même (bornes/milieu/débordement) est couverte par les tests `pitchForZoom`.
        val state = ChaseCameraController(config).cameraFor(paris, bearingDeg = 0.0, userZoom = 25.0)
        state.zoomLevel shouldBe 19.0
        state.pitchDeg shouldBe 70.0
    }

    "quand la plage de zoom est nulle, le pitch reste défini (pitch minimal)" {
        val flatConfig = configOf(followZoom = 18.0, minZoom = 18.0, maxZoom = 18.0)
        val state = ChaseCameraController(flatConfig).cameraFor(paris, bearingDeg = 0.0)
        state.pitchDeg shouldBe 20.0
    }

    "pitchForZoom expose la courbe : bornes min/max et milieu interpolé" {
        val controller = ChaseCameraController(config)
        controller.pitchForZoom(14.0) shouldBe 20.0
        controller.pitchForZoom(19.0) shouldBe 70.0
        controller.pitchForZoom(16.5) shouldBe 45.0
    }

    "pitchForZoom borne lui-même un zoom hors plage avant d'interpoler" {
        val controller = ChaseCameraController(config)
        controller.pitchForZoom(25.0) shouldBe 70.0
        controller.pitchForZoom(5.0) shouldBe 20.0
    }

    "un zoom utilisateur dans les bornes est respecté" {
        val state = ChaseCameraController(config).cameraFor(paris, bearingDeg = 0.0, userZoom = 16.0)
        state.zoomLevel shouldBe 16.0
    }

    "un zoom utilisateur en-deçà du minimum est borné" {
        val state = ChaseCameraController(config).cameraFor(paris, bearingDeg = 0.0, userZoom = 5.0)
        state.zoomLevel shouldBe 14.0
    }

    "rejette une configuration aux bornes de zoom incohérentes" {
        shouldThrow<IllegalArgumentException> { configOf(minZoom = 19.0, maxZoom = 14.0) }
    }

    "rejette une configuration dont le zoom de poursuite sort des bornes" {
        shouldThrow<IllegalArgumentException> { configOf(followZoom = 25.0) }
    }

    "rejette une configuration dont le pitch minimal dépasse le pitch maximal" {
        shouldThrow<IllegalArgumentException> { configOf(minPitchDeg = 70.0, maxPitchDeg = 20.0) }
    }
})
