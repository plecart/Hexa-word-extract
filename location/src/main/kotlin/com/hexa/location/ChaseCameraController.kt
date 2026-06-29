package com.hexa.location

import com.hexa.core.geo.LatLng

/**
 * Politique **pure** de la caméra de poursuite à la troisième personne.
 *
 * À partir de la position suivie, du cap (piloté par le geste de rotation) et d'un éventuel zoom
 * utilisateur, il décide la pose de caméra à imposer. La caméra reste **verrouillée en permanence sur le joueur** : pas de mode
 * libre, aucune pose nulle, aucun décentrage possible. Sans état mutable ni dépendance Mapbox, ce qui
 * rend le comportement testable isolément. Le câblage (flux, gestes, rendu) vit dans `:app`.
 */
class ChaseCameraController(private val config: ChaseCameraConfig) {

    /**
     * Pose de caméra centrée sur le joueur.
     *
     * Centre sur [position], applique le cap fourni, un zoom borné à `[minZoom, maxZoom]` ([userZoom]
     * s'il est fourni, sinon `followZoom`), et un **pitch couplé à ce zoom borné** (cf. [pitchForZoom]).
     *
     * @param position position suivie sur laquelle centrer.
     * @param bearingDeg cap de la caméra en degrés, attendu dans `[0, 360)` — piloté par le geste de
     *   rotation de l'utilisateur (câblage dans `:app`).
     * @param userZoom zoom choisi au pincement ; `null` tant que l'utilisateur n'a pas ajusté.
     * @return la pose à imposer ; jamais nulle, la caméra suit toujours le joueur.
     */
    fun cameraFor(position: LatLng, bearingDeg: Double, userZoom: Double? = null): CameraState {
        val zoom = (userZoom ?: config.followZoom).coerceIn(config.minZoom, config.maxZoom)
        return CameraState(
            center = position,
            zoomLevel = zoom,
            pitchDeg = pitchForZoom(zoom),
            bearingDeg = bearingDeg,
        )
    }

    /**
     * Pitch couplé au zoom : interpolation **linéaire** entre [ChaseCameraConfig.minPitchDeg] (au zoom
     * le plus large, vue plongeante) et [ChaseCameraConfig.maxPitchDeg] (au zoom le plus rapproché, vue
     * rasante). Croissant continûment avec le zoom, sans palier.
     *
     * [zoom] est le zoom **déjà borné** à `[minZoom, maxZoom]` par [cameraFor] : la fraction
     * d'interpolation reste donc dans `[0, 1]` et le pitch dans `[minPitchDeg, maxPitchDeg]` — borné
     * sans clamp supplémentaire, même si le zoom utilisateur débordait. Si la plage de zoom est nulle
     * (`minZoom == maxZoom`, config dégénérée), il n'y a pas de zoom à interpoler : on rend le pitch
     * minimal.
     */
    private fun pitchForZoom(zoom: Double): Double {
        val zoomSpan = config.maxZoom - config.minZoom
        if (zoomSpan == 0.0) return config.minPitchDeg
        val fraction = (zoom - config.minZoom) / zoomSpan
        return config.minPitchDeg + fraction * (config.maxPitchDeg - config.minPitchDeg)
    }
}
