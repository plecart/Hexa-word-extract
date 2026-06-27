package com.hexa.core.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distance géodésique entre deux positions, modèle sphérique (formule de haversine).
 *
 * Sert à comparer la proximité du joueur aux centres de deux cellules voisines : c'est la mesure
 * sur laquelle repose l'hystérésis de tuile courante (on ne bascule de tuile que si le centre de la
 * candidate est *franchement* plus proche que celui de la tuile courante). Aux échelles du jeu
 * (quelques dizaines de mètres entre cellules H3 res-11), l'écart au modèle ellipsoïdal réel est
 * négligeable, et la haversine reste stable même pour de très petites distances.
 */
object GreatCircle {
    /** Rayon terrestre moyen, en mètres (sphère de référence WGS84). */
    private const val EARTH_RADIUS_M = 6_371_008.8

    /**
     * Distance en mètres le long du grand cercle entre [a] et [b].
     *
     * @return distance non négative ; `0` pour deux positions identiques. Symétrique en ses arguments.
     */
    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val lat1 = a.latDeg * DEGREES_TO_RADIANS
        val lat2 = b.latDeg * DEGREES_TO_RADIANS
        val dLat = (b.latDeg - a.latDeg) * DEGREES_TO_RADIANS
        val dLng = (b.lngDeg - a.lngDeg) * DEGREES_TO_RADIANS
        val sinLat = sin(dLat / 2)
        val sinLng = sin(dLng / 2)
        val h = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLng * sinLng
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }
}
