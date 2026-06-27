package com.hexa.core.geo

import kotlin.math.PI

/**
 * Facteur de conversion **degrés → radians** (`π / 180`), partagé par les conversions d'angles du
 * package `geo` ([GreatCircle], [UnitSphere]). Une seule définition fait foi, au lieu d'une copie
 * `private` par fichier qui pourrait diverger.
 */
internal const val DEGREES_TO_RADIANS = PI / 180.0

/** Mesure d'un tour complet en degrés (`360°`) — la période de [wrapDegrees]. */
const val FULL_TURN_DEGREES = 360.0

/**
 * Ramène un angle exprimé en degrés dans l'intervalle canonique `[0, 360)`, en gérant correctement
 * les valeurs négatives et celles dépassant un tour complet (`(x mod 360 + 360) mod 360`).
 *
 * Point de wrap **unique** du projet : tout angle en degrés à ramener dans `[0, 360)` (lissage de
 * cap, source boussole…) passe par ici plutôt que d'en recopier la formule. `public` à dessein
 * (à la différence de [DEGREES_TO_RADIANS]) car consommé hors de `:core`.
 *
 * @receiver angle en degrés, quelconque (négatif, supérieur à 360°, multiple de tours).
 * @return l'angle équivalent dans `[0, 360)` : `0.0` pour un multiple exact de `360°`.
 */
fun Double.wrapDegrees(): Double = ((this % FULL_TURN_DEGREES) + FULL_TURN_DEGREES) % FULL_TURN_DEGREES
