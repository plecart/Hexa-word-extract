package com.hexa.map

import com.hexa.core.geo.LatLng

/**
 * État visuel d'une cellule de la grille : avec son contenu, il détermine la teinte de remplissage de
 * la tuile (cf. [tileFillColor]).
 *
 * - [NORMALE] : cellule ordinaire — teintée par son contenu (élément le plus rare, ou neutre si vide).
 * - [COURANTE] : cellule sous le joueur — porte un **léger surlignage** distinct des teintes de
 *   ressource, pour rester repérable (#126).
 *
 * Les tuiles **bâties** ne sont pas distinguées ici : un bâtiment posé est rendu par son **modèle 3D**
 * (cf. [Style.showBuildingModels]).
 */
enum class TileState {
    NORMALE,
    COURANTE,
}

/**
 * Une cellule de la grille prête à dessiner : son [outline] (sommets du contour) et sa [fillColorRgba]
 * (teinte de remplissage déjà résolue depuis l'état et le contenu, cf. [tileFillColor]).
 *
 * @property outline sommets du contour de la cellule, dans l'ordre (cf. [HexGrid.outline]).
 * @property fillColorRgba couleur de remplissage au format `rgba(r, g, b, a)`, prête pour `fill-color`.
 */
data class GridCell(val outline: List<LatLng>, val fillColorRgba: String)

/**
 * Classe une cellule en son état visuel : la cellule sous le joueur est [TileState.COURANTE], toutes
 * les autres sont [TileState.NORMALE].
 *
 * @param cell cellule à classer.
 * @param current cellule courante (sous le joueur).
 */
fun tileState(cell: Long, current: Long): TileState = if (cell == current) TileState.COURANTE else TileState.NORMALE
