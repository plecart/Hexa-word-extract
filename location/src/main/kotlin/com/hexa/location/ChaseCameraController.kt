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
     * Centre sur [position], applique le pitch configuré et le cap fourni, et un zoom borné à
     * `[minZoom, maxZoom]` ([userZoom] s'il est fourni, sinon `followZoom`).
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
            pitchDeg = config.pitchDeg,
            bearingDeg = bearingDeg,
        )
    }
}
