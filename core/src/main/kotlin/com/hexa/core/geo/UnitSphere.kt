package com.hexa.core.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Projection d'une position géographique (latitude, longitude) sur la sphère unité.
 *
 * Échantillonner le bruit procédural directement sur la sphère 3D — plutôt que sur une grille
 * (lat, lng) plate — élimine deux artefacts d'une carte projetée :
 * - **la couture à l'antiméridien** : −180° et +180° désignent le même méridien et donnent donc
 *   le même point (les fonctions trigonométriques sont périodiques de période 360°) ;
 * - **la distorsion aux pôles** : à ±90° de latitude, toutes les longitudes convergent vers un
 *   unique point ((0, 0, ±1)), sans étirement.
 *
 * Convention : longitude 0° → +X, +90° → +Y, latitude +90° (pôle Nord) → +Z.
 */
object UnitSphere {
    private const val DEGREES_TO_RADIANS = PI / 180.0

    /**
     * Convertit une position géographique en vecteur unitaire sur la sphère.
     *
     * Le vecteur retourné est toujours de norme 1, pour toute entrée (y compris hors des plages
     * usuelles : la périodicité des fonctions trigonométriques absorbe les longitudes au-delà de
     * ±180° ou les latitudes au-delà de ±90° sans erreur).
     *
     * @param latDeg latitude en degrés ; +90° = pôle Nord, −90° = pôle Sud.
     * @param lngDeg longitude en degrés ; +180° et −180° désignent le même méridien.
     * @return le point correspondant sur la sphère unité (norme 1).
     */
    fun fromLatLng(latDeg: Double, lngDeg: Double): Vector3 {
        val lat = latDeg * DEGREES_TO_RADIANS
        val lng = lngDeg * DEGREES_TO_RADIANS
        val cosLat = cos(lat)
        return Vector3(
            x = cosLat * cos(lng),
            y = cosLat * sin(lng),
            z = sin(lat),
        )
    }
}
