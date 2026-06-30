package com.hexa.map

/**
 * Logique **pure** de sélection des cellules de la grille à afficher autour du joueur.
 *
 * Sépare la décision « combien de cellules, et lesquelles » de toute dépendance native : le rayon
 * (en anneaux) se déduit du zoom via les paliers de [MapConfig], et l'énumération des cellules est
 * déléguée à une [HexGrid]. Testable sans la bibliothèque H3, avec une fausse grille.
 */
object VisibleCells {
    /**
     * Nombre d'anneaux du disque de cellules à dessiner au [zoom] donné, lu depuis les paliers
     * [MapConfig.GRID_RING_STEPS] : décroissant avec le zoom, borné par
     * [MapConfig.GRID_MIN_RINGS]–[MapConfig.GRID_MAX_RINGS].
     */
    fun ringsForZoom(zoom: Double): Int =
        MapConfig.GRID_RING_STEPS.firstOrNull { zoom >= it.first }?.second ?: MapConfig.GRID_MAX_RINGS

    /**
     * Cellules H3 à dessiner autour de [center] au [zoom] donné : le disque dont le rayon est
     * [ringsForZoom] anneaux (centre inclus).
     *
     * @param center cellule courante (contenant la position du joueur).
     * @param zoom niveau de zoom courant de la carte.
     * @param grid grille hexagonale fournissant le disque de cellules.
     */
    fun cellsAround(center: Long, zoom: Double, grid: HexGrid): List<Long> = grid.disk(center, ringsForZoom(zoom))
}
