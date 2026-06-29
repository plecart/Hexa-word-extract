package com.hexa.map

import com.hexa.core.geo.LatLng

/**
 * État visuel d'une cellule de la grille, **porté dans la source** de la grille ([Style.showHexGrid])
 * comme fondation du rendu.
 *
 * - [NORMALE] : cellule ordinaire de la grille.
 * - [COURANTE] : cellule sous le joueur, destinée à un léger surlignage.
 *
 * Depuis #125, l'habillage cyan est retiré et **aucune couche ne rend encore cet état** ; #126
 * (recoloration des hexagones) rebranchera un remplissage piloté par cet état (léger surlignage de la
 * tuile courante) et par le contenu de la tuile. Les tuiles **bâties** ne sont pas distinguées ici :
 * un bâtiment posé est rendu par son **modèle 3D** (cf. [Style.showBuildingModels]).
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
