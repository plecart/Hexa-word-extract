package com.hexa.map

import com.hexa.location.ChaseCameraConfig
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.time.Duration

/**
 * Garde-fous sur les réglages caméra de [MapConfig] et le trajet de démonstration : ce sont des
 * valeurs, mais leur **cohérence** est un contrat (pitch troisième personne, zoom de poursuite dans
 * les bornes de jeu, lissage normalisé, trajet rejouable). On vérifie ce contrat plutôt que les
 * nombres exacts, qui restent libres de réglage.
 */
class MapCameraConfigTest : StringSpec({
    "le pitch de poursuite reste dans la plage troisième personne (55–65°)" {
        (MapConfig.PITCH in 55.0..65.0) shouldBe true
    }

    "les réglages caméra forment une configuration de contrôleur valide" {
        // ChaseCameraConfig valide ses invariants (zoom de poursuite dans les bornes) à la construction.
        shouldNotThrowAny {
            ChaseCameraConfig(
                pitchDeg = MapConfig.PITCH,
                followZoom = MapConfig.FOLLOW_ZOOM,
                minZoom = MapConfig.MIN_ZOOM,
                maxZoom = MapConfig.MAX_ZOOM,
            )
        }
    }

    "le coefficient de lissage du cap est un facteur normalisé non nul" {
        (MapConfig.HEADING_SMOOTHING_FACTOR > 0.0 && MapConfig.HEADING_SMOOTHING_FACTOR <= 1.0) shouldBe true
    }

    "le trajet de démonstration est rejouable (non vide, pas strictement positif)" {
        DemoTrajectory.POINTS.shouldNotBeEmpty()
        (DemoTrajectory.STEP > Duration.ZERO) shouldBe true
    }
})
