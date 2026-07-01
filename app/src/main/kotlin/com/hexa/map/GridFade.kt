package com.hexa.map

/**
 * Fondu **pur** de la grille avec l'éloignement au joueur : facteur d'opacité dans `[0, 1]` appliqué à
 * une cellule selon sa distance en anneaux H3 à la tuile courante ([HexGrid.gridDistance]).
 *
 * Décroît **linéairement** de 1 (tuile du joueur, distance 0) vers ~0 au bord du rayon rendu
 * ([MapConfig.GRID_RENDER_RINGS]) : les tuiles lointaines s'effacent, la grille ne surcharge pas la
 * carte. Ce facteur se **compose** (multiplication) avec l'alpha de teinte posé par #126 ([tileFillColor]) —
 * il ne le remplace pas.
 *
 * Pur et testé isolément ; la validation de la courbe à l'œil relève de la DA. **Provisoire.**
 */
object GridFade {
    /**
     * Facteur d'opacité (dans `[0, 1]`) d'une cellule à [distanceRings] anneaux du joueur : `1` au
     * centre, décroissant, quasi nul (mais non nul) au bord du rayon rendu.
     *
     * Le dénominateur `GRID_RENDER_RINGS + 1` laisse l'anneau du bord faiblement visible (`1/6` au
     * rayon 5) plutôt que de l'effacer complètement ; composé à l'alpha de teinte (0,05–0,25), il
     * retombe alors sous le seuil du perceptible. Le résultat est borné défensivement à `[0, 1]` : une
     * distance négative (cas H3 dégénéré près d'un pentagone) retombe dans les bornes au lieu de déborder.
     */
    fun factorFor(distanceRings: Int): Float =
        (1f - distanceRings.toFloat() / (MapConfig.GRID_RENDER_RINGS + 1)).coerceIn(0f, 1f)
}
