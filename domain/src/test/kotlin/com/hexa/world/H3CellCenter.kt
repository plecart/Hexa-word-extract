package com.hexa.world

import com.hexa.config.GameConfig
import com.hexa.core.geo.LatLng
import com.uber.h3core.H3Core

/**
 * Adaptateur H3 **réservé aux tests** : il câble le port [TileCenterLocator] sur la bibliothèque
 * native `com.uber:h3` et sert d'usine à indices H3 (résolution du jeu).
 *
 * Le code de production de `:domain` ne dépend jamais de H3 : il reçoit un [TileCenterLocator]
 * par injection. Cet adaptateur prouve que le port s'implémente trivialement au-dessus de la vraie
 * grille, tout en gardant le natif hors du classpath de production (cf. `domain/build.gradle.kts`).
 */
class H3CellCenter : TileCenterLocator {
    private val h3 = H3Core.newInstance()

    override fun centerOf(h3Index: Long): LatLng {
        val center = h3.cellToLatLng(h3Index)
        return LatLng(latDeg = center.lat, lngDeg = center.lng)
    }

    /** Index H3 (à [GameConfig.H3_RESOLUTION]) de la tuile contenant la position donnée. */
    fun cellAt(latDeg: Double, lngDeg: Double): Long = h3.latLngToCell(latDeg, lngDeg, GameConfig.H3_RESOLUTION)

    /** Disque de cellules autour d'une cellule centrale : `rings` anneaux (4 anneaux → 61 cellules). */
    fun disk(center: Long, rings: Int): List<Long> = h3.gridDisk(center, rings)
}
