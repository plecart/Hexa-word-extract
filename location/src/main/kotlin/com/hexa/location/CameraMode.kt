package com.hexa.location

/**
 * Mode de pilotage de la caméra de poursuite.
 *
 * - [FOLLOW] : la caméra suit la position courante ; le contrôleur impose une [CameraState].
 * - [FREE] : l'utilisateur a déplacé la carte à la main ; le contrôleur rend la main et n'impose
 *   plus de pose, jusqu'à un recentrage explicite.
 */
enum class CameraMode {
    FOLLOW,
    FREE,
}
