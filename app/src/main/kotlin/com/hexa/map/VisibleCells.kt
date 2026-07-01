package com.hexa.map

/**
 * Logique **pure** de sélection des cellules de la grille à afficher autour du joueur.
 *
 * Le rendu couvre un **rayon fixe** de [MapConfig.GRID_RENDER_RINGS] anneaux, indépendant du zoom :
 * l'énumération des cellules est déléguée à une [HexGrid]. Testable sans la bibliothèque H3, avec une
 * fausse grille.
 */
object VisibleCells {
    /**
     * Cellules H3 à dessiner autour de [center] : le disque de **rayon fixe** [MapConfig.GRID_RENDER_RINGS]
     * anneaux (centre inclus), quel que soit le zoom. Le fondu avec la distance ([GridFade]) se charge
     * d'estomper les tuiles lointaines.
     *
     * @param center cellule courante (contenant la position du joueur).
     * @param grid grille hexagonale fournissant le disque de cellules.
     */
    fun cellsAround(center: Long, grid: HexGrid): List<Long> = grid.disk(center, MapConfig.GRID_RENDER_RINGS)
}
