package com.hexa.map

/**
 * Ensemble des cellules **bâties** — celles portant un bâtiment du joueur (cf. PRD #2, F2/F5).
 *
 * Petite interface (un seul test d'appartenance), pour découpler la grille de la **source** des
 * bâtiments. Au MVP, la source réelle (documents `players/{id}/buildings/{h3Index}` de Firestore)
 * n'existe pas encore : [DemoBuiltTiles] fournit un jeu de cellules factices pour démontrer le
 * troisième état visuel. Le passage aux vraies données ne remplacera que l'implémentation.
 */
fun interface BuiltTiles {
    /** `true` si la cellule [cell] (index H3) porte un bâtiment. */
    fun contains(cell: Long): Boolean
}

/**
 * Source **factice** de tuiles bâties, le temps que les vraies données de bâtiments arrivent.
 *
 * Marque comme bâtie environ une cellule sur quatre, par un critère **déterministe** sur l'index H3
 * (même cellule → même verdict d'une session à l'autre) : des cellules voisines diffèrent — leurs
 * digits H3 de fin varient —, ce qui éparpille les tuiles bâties dans le champ et rend les trois
 * états visuels immédiatement démontrables, sans dépendre d'aucune donnée stockée.
 */
object DemoBuiltTiles : BuiltTiles {
    /** Une cellule sur quatre : les deux bits de poids faible du repli 64→32 bits de l'index valent 0. */
    override fun contains(cell: Long): Boolean = ((cell xor (cell ushr 32)) and 0x3L) == 0L
}
