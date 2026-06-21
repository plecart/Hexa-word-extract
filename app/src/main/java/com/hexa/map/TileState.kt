package com.hexa.map

import com.hexa.core.geo.LatLng

/**
 * État visuel d'une cellule de la grille, qui détermine son rendu (cf. [Style.showHexGrid]).
 *
 * - [NORMALE] : cellule ordinaire de la grille (contour seul).
 * - [COURANTE] : cellule sous le joueur, surlignée distinctement.
 * - [BATIE] : cellule portant un bâtiment du joueur, distincte des deux autres (cf. [BuiltTiles]).
 */
enum class TileState {
    NORMALE,
    COURANTE,
    BATIE,
}

/**
 * Une cellule de la grille prête à dessiner : son [outline] (sommets du contour) et son [state].
 *
 * @property outline sommets du contour de la cellule, dans l'ordre (cf. [HexGrid.outline]).
 * @property state état visuel de la cellule.
 */
data class GridCell(val outline: List<LatLng>, val state: TileState)

/**
 * Classe une cellule en son état visuel. La **tuile courante prime** : la cellule sous le joueur est
 * toujours [TileState.COURANTE], même si elle est bâtie (on veut toujours voir où l'on se trouve).
 *
 * @param cell cellule à classer.
 * @param current cellule courante (sous le joueur).
 * @param builtTiles ensemble des cellules bâties.
 */
fun tileState(cell: Long, current: Long, builtTiles: BuiltTiles): TileState = when {
    cell == current -> TileState.COURANTE
    builtTiles.contains(cell) -> TileState.BATIE
    else -> TileState.NORMALE
}
