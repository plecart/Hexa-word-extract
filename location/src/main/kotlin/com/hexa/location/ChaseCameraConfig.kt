package com.hexa.location

/**
 * Paramètres de cadrage de la caméra de poursuite, **injectés** depuis la configuration de
 * présentation de l'app (`com.hexa.map.MapConfig`). Le module `:location` étant pur, il ne lit pas
 * cette configuration : il la reçoit, ce qui garde la logique testable hors device tout en laissant
 * `MapConfig` comme point unique de réglage des valeurs.
 *
 * @property pitchDeg inclinaison de la caméra en degrés (vue troisième personne).
 * @property followZoom zoom appliqué en poursuite tant que l'utilisateur n'a pas pincé pour ajuster.
 * @property minZoom zoom minimal autorisé au pincement.
 * @property maxZoom zoom maximal autorisé au pincement.
 * @throws IllegalArgumentException si `minZoom > maxZoom` ou si [followZoom] sort de `[minZoom,
 *   maxZoom]`.
 */
data class ChaseCameraConfig(
    val pitchDeg: Double,
    val followZoom: Double,
    val minZoom: Double,
    val maxZoom: Double,
) {
    init {
        require(minZoom <= maxZoom) { "minZoom ($minZoom) doit être ≤ maxZoom ($maxZoom)" }
        require(followZoom in minZoom..maxZoom) {
            "followZoom ($followZoom) doit être dans [$minZoom, $maxZoom]"
        }
    }
}
