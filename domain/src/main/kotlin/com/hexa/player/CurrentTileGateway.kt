package com.hexa.player

/**
 * Port de la **tuile courante** du joueur : la cellule H3 (index textuel) sous sa position.
 *
 * Source **autoritaire** pour vérifier qu'une pose vise bien « sous les pieds » du joueur. Le domaine
 * l'interroge lui-même plutôt que de faire confiance à l'appelant pour cette garde (couture de
 * sécurité, cf. #137) : un futur backend à autorité serveur la dérivera de la position rapportée ;
 * cette tranche la câble sur le suivi GPS (côté `:app`, cf. [com.hexa.HexaApplication.sharedCurrentTile]).
 */
fun interface CurrentTileGateway {
    /**
     * Index H3 **textuel** de la tuile courante du joueur, ou `null` tant qu'aucune position n'est
     * connue (auquel cas aucune tuile n'est « courante » et toute pose est refusée).
     */
    fun currentCell(): String?
}
