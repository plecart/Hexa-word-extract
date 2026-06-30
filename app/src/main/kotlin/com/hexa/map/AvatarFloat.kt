package com.hexa.map

import kotlin.math.PI
import kotlin.math.sin

/** Un cycle complet en radians — facteur de conversion temps écoulé → phase de la sinusoïde. */
private const val FULL_CYCLE_RADIANS = 2 * PI

/**
 * Décalage vertical de **flottement** de l'avatar à l'instant [elapsedMs], en mètres.
 *
 * Oscillation sinusoïdale pure centrée sur zéro — l'« effet fantôme » de #98 :
 * `offset(t) = amplitude · sin(2π · t / période)`. La courbe est douce aux extrémités (vitesse nulle
 * aux extrema, maximale au passage par le repos) et boucle indéfiniment avec la période donnée.
 *
 * Fonction **pure** : aucune dépendance Mapbox/Android, donc directement testable. L'ancrage au sol et
 * la **hauteur de repos** (qui garantit que l'avatar ne s'enfonce jamais sous le sol) sont ajoutés au
 * rendu par la couche modèle de l'avatar (cf. [Style.showAvatar]) ; ils ne font pas partie de cette
 * oscillation, qui reste centrée sur zéro.
 *
 * @param elapsedMs temps écoulé depuis le début de l'animation, en millisecondes (même unité que
 *   [periodMs]). À `t = 0` l'offset est nul : l'avatar démarre à sa position de repos.
 * @param amplitudeM amplitude de l'oscillation, en mètres : l'offset reste dans `[-amplitudeM, +amplitudeM]`.
 * @param periodMs durée d'un cycle complet, en millisecondes. **Doit être > 0** ; une valeur nulle ou
 *   négative renvoie `0.0` (garde anti-division par zéro), ce qui désactive proprement le flottement.
 * @return le décalage vertical à appliquer, en mètres (positif = vers le haut).
 */
internal fun avatarFloatOffsetMeters(elapsedMs: Double, amplitudeM: Double, periodMs: Double): Double {
    if (periodMs <= 0.0) return 0.0
    return amplitudeM * sin(FULL_CYCLE_RADIANS * elapsedMs / periodMs)
}
