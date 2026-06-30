package com.hexa.map

import com.hexa.core.geo.LatLng

/**
 * Une cellule de la grille prête à dessiner : son [outline] (sommets du contour) et sa [fillColorRgba]
 * (teinte de remplissage déjà résolue depuis son contenu, cf. [tileFillColor]).
 *
 * Les tuiles **bâties** ne sont pas distinguées ici : un bâtiment posé est rendu par son **modèle 3D**
 * (cf. [Style.showBuildingModels]). La tuile **sous le joueur** non plus : elle se colore comme les
 * autres, l'avatar 3D marquant déjà la position du joueur.
 *
 * @property outline sommets du contour de la cellule, dans l'ordre (cf. [HexGrid.outline]).
 * @property fillColorRgba couleur de remplissage au format `rgba(r, g, b, a)`, prête pour `fill-color`.
 */
data class GridCell(val outline: List<LatLng>, val fillColorRgba: String)
