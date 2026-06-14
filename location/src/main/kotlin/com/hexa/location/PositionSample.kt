package com.hexa.location

import com.hexa.core.geo.LatLng

/**
 * Mesure de position brute issue d'une source de localisation, avant filtrage.
 *
 * Associe la position à sa **précision** annoncée, indispensable pour rejeter les points peu fiables
 * dans [PositionFilter]. [com.hexa.core.geo.LatLng] ne porte pas la précision (c'est un simple type
 * géographique) : ce type valeur l'enrichit, et reste local à `:location` car l'accuracy n'a de sens
 * que pour le filtrage de la source.
 *
 * @property position position géographique mesurée.
 * @property accuracyM rayon de précision horizontale estimé, en mètres (plus petit = plus fiable).
 */
data class PositionSample(val position: LatLng, val accuracyM: Double)
