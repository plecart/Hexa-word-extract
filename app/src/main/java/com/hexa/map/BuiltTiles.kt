package com.hexa.map

/**
 * Ensemble des cellules **bâties** — celles portant un bâtiment du joueur (cf. PRD #2, F2/F5).
 *
 * Petite interface (un seul test d'appartenance), pour découpler la classification d'état des tuiles
 * ([tileState]) de la **source** des bâtiments. Au MVP, cette source est la base posée
 * (`Player.baseCell`) : [HexGridViewModel] construit l'ensemble à partir du flux des index H3 bâtis.
 */
fun interface BuiltTiles {
    /** `true` si la cellule [cell] (index H3) porte un bâtiment. */
    fun contains(cell: Long): Boolean
}
