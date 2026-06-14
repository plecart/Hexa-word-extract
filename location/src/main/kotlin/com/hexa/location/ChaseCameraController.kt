package com.hexa.location

import com.hexa.core.geo.LatLng

/**
 * Politique **pure** de la caméra de poursuite à la troisième personne.
 *
 * À partir du [mode] courant, de la position suivie, du cap lissé et d'un éventuel zoom utilisateur,
 * il décide la pose de caméra à appliquer — ou de **rendre la main** à l'utilisateur en mode libre.
 * Sans état mutable ni dépendance Mapbox : les transitions de mode renvoient un nouveau contrôleur,
 * ce qui rend chaque comportement testable isolément. Le câblage (flux, gestes, rendu) vit dans
 * `:app`.
 *
 * @property mode mode courant ; un contrôleur neuf démarre en [CameraMode.FOLLOW].
 */
class ChaseCameraController private constructor(
    private val config: ChaseCameraConfig,
    val mode: CameraMode,
) {
    constructor(config: ChaseCameraConfig) : this(config, CameraMode.FOLLOW)

    /** Passe en mode libre — à appeler quand l'utilisateur déplace la carte au doigt. */
    fun releasedByGesture(): ChaseCameraController = ChaseCameraController(config, CameraMode.FREE)

    /** Réengage la poursuite — à appeler sur le bouton de recentrage. */
    fun recentered(): ChaseCameraController = ChaseCameraController(config, CameraMode.FOLLOW)

    /**
     * Pose de caméra pour l'état courant.
     *
     * En [CameraMode.FOLLOW], centre sur [position], applique le pitch et le cap fournis et un zoom
     * borné à `[minZoom, maxZoom]` ([userZoom] s'il est fourni, sinon `followZoom`). En
     * [CameraMode.FREE], renvoie `null` : la caméra reste là où l'utilisateur l'a laissée.
     *
     * @param position position suivie sur laquelle centrer.
     * @param headingDeg cap lissé en degrés, attendu dans `[0, 360)` (cf. [HeadingSmoother]).
     * @param userZoom zoom choisi au pincement ; `null` tant que l'utilisateur n'a pas ajusté.
     * @return la pose à imposer en poursuite, ou `null` en mode libre.
     */
    fun cameraFor(position: LatLng, headingDeg: Double, userZoom: Double? = null): CameraState? {
        if (mode == CameraMode.FREE) return null
        val zoom = (userZoom ?: config.followZoom).coerceIn(config.minZoom, config.maxZoom)
        return CameraState(
            center = position,
            zoomLevel = zoom,
            pitchDeg = config.pitchDeg,
            bearingDeg = headingDeg,
        )
    }
}
