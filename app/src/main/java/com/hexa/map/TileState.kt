package com.hexa.map

import com.hexa.core.geo.LatLng

/**
 * État visuel d'une cellule de la grille, qui détermine son rendu (cf. [Style.showHexGrid]).
 *
 * - [NORMALE] : cellule ordinaire de la grille (contour seul).
 * - [COURANTE] : cellule sous le joueur, surlignée distinctement.
 *
 * Les tuiles **bâties** ne sont plus distinguées sur la grille : un bâtiment posé est rendu par son
 * **modèle 3D** (cf. [Style.showBuildingModels]), qui rend le remplissage redondant.
 */
enum class TileState {
    NORMALE,
    COURANTE,
}

/**
 * Une cellule de la grille prête à dessiner : son [outline] (sommets du contour) et son [state].
 *
 * @property outline sommets du contour de la cellule, dans l'ordre (cf. [HexGrid.outline]).
 * @property state état visuel de la cellule.
 */
data class GridCell(val outline: List<LatLng>, val state: TileState)

/**
 * Classe une cellule en son état visuel : la cellule sous le joueur est [TileState.COURANTE], toutes
 * les autres sont [TileState.NORMALE].
 *
 * @param cell cellule à classer.
 * @param current cellule courante (sous le joueur).
 */
fun tileState(cell: Long, current: Long): TileState = if (cell == current) TileState.COURANTE else TileState.NORMALE
