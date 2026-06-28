package com.hexa.map

import com.hexa.location.ChaseCameraConfig
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Garde-fous sur les réglages caméra et GPS de [MapConfig] : ce sont des valeurs, mais leur
 * **cohérence** est un contrat (pitch troisième personne, zoom de poursuite dans les bornes de jeu,
 * coefficients de lissage normalisés, seuils positifs). On vérifie ce contrat plutôt que les nombres
 * exacts, qui restent libres de réglage.
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

    "le coefficient de lissage de position est un facteur normalisé non nul" {
        (MapConfig.POSITION_SMOOTHING_FACTOR > 0.0 && MapConfig.POSITION_SMOOTHING_FACTOR <= 1.0) shouldBe true
    }

    "le seuil de précision GPS et l'intervalle de mise à jour sont strictement positifs" {
        (MapConfig.ACCURACY_THRESHOLD_M > 0.0) shouldBe true
        (MapConfig.GPS_INTERVAL_MS > 0L) shouldBe true
    }

    "le modèle 3D de l'avatar forme une configuration valide" {
        (MapConfig.AVATAR_MODEL_SCALE > 0.0) shouldBe true
        (MapConfig.AVATAR_MODEL_GROUND_LIFT_M >= 0.0) shouldBe true
        MapConfig.AVATAR_MODEL_GLB.isNotBlank() shouldBe true
    }

    "le flottement de l'avatar forme une configuration valide (repos détaché du sol, jamais sous le sol)" {
        (MapConfig.AVATAR_FLOAT_AMPLITUDE_M > 0.0) shouldBe true
        (MapConfig.AVATAR_FLOAT_PERIOD_MS > 0L) shouldBe true
        // Repos strictement au-dessus du sol ET ≥ amplitude : au bas du cycle (offset = −amplitude),
        // le modèle reste à sa position d'ancrage au sol ou au-dessus — il ne s'enfonce jamais.
        (MapConfig.AVATAR_FLOAT_REST_HEIGHT_M > 0.0) shouldBe true
        (MapConfig.AVATAR_FLOAT_REST_HEIGHT_M >= MapConfig.AVATAR_FLOAT_AMPLITUDE_M) shouldBe true
    }
})
