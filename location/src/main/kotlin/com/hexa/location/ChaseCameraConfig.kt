package com.hexa.location

/**
 * Paramètres de cadrage de la caméra de poursuite, **injectés** depuis la configuration de
 * présentation de l'app (`com.hexa.map.MapConfig`). Le module `:location` étant pur, il ne lit pas
 * cette configuration : il la reçoit, ce qui garde la logique testable hors device tout en laissant
 * `MapConfig` comme point unique de réglage des valeurs.
 *
 * Le pitch n'est plus une valeur unique : il est **couplé au zoom** par [ChaseCameraController], qui
 * interpole continûment entre [minPitchDeg] (au zoom le plus large, vue plongeante) et [maxPitchDeg]
 * (au zoom le plus rapproché, vue rasante vers l'horizon). Cette config ne porte que les **bornes** ;
 * la loi d'interpolation est la responsabilité du contrôleur.
 *
 * @property minPitchDeg inclinaison de la caméra en degrés au **zoom minimal** (vue la plus large) —
 *   pitch le plus faible, quasi plongeant / top-down.
 * @property maxPitchDeg inclinaison de la caméra en degrés au **zoom maximal** (vue la plus
 *   rapprochée) — pitch le plus élevé, caméra redressée vers l'horizon.
 * @property followZoom zoom appliqué en poursuite tant que l'utilisateur n'a pas pincé pour ajuster.
 * @property minZoom zoom minimal autorisé au pincement.
 * @property maxZoom zoom maximal autorisé au pincement.
 * @throws IllegalArgumentException si `minPitchDeg > maxPitchDeg`, si `minZoom > maxZoom`, ou si
 *   [followZoom] sort de `[minZoom, maxZoom]`.
 */
data class ChaseCameraConfig(
    val minPitchDeg: Double,
    val maxPitchDeg: Double,
    val followZoom: Double,
    val minZoom: Double,
    val maxZoom: Double,
) {
    init {
        require(minPitchDeg <= maxPitchDeg) {
            "minPitchDeg ($minPitchDeg) doit être ≤ maxPitchDeg ($maxPitchDeg)"
        }
        require(minZoom <= maxZoom) { "minZoom ($minZoom) doit être ≤ maxZoom ($maxZoom)" }
        require(followZoom in minZoom..maxZoom) {
            "followZoom ($followZoom) doit être dans [$minZoom, $maxZoom]"
        }
    }
}
