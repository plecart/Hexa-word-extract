package com.hexa.player

/**
 * Craft d'un bâtiment et **persistance** du résultat (cf. PRD #5, user stories 8-11).
 *
 * Glu mince autour du module pur [Craft] : charge le document joueur, délègue la décision (suffisance,
 * débit, crédit), et n'écrit que sur un succès. Un craft refusé faute de ressources **ne touche pas
 * Firestore** — l'UI gardant le bouton désactivé, ce cas est une sécurité, pas le chemin nominal.
 *
 * @property auth source de l'identité (compte anonyme), pour retrouver l'uid du document joueur.
 * @property players accès au document joueur (lecture + écriture).
 */
class CraftBuildingUseCase(
    private val auth: AuthGateway,
    private val players: PlayerRepository,
) {
    /**
     * Construit un [type] pour le joueur courant : débite l'inventaire et crédite le stock, puis
     * persiste. Sans effet si les ressources manquent.
     *
     * @param type bâtiment à construire (détermine la recette, cf. [Craft]).
     * @throws IllegalStateException si le document joueur n'existe pas encore (amorçage requis avant
     *   tout craft, cf. [EnsurePlayerUseCase]).
     */
    suspend operator fun invoke(type: BuildingType) {
        val id = auth.ensureSignedIn()
        val player =
            players.load(id)
                ?: error("Craft impossible : document joueur absent (amorçage requis).")
        when (val outcome = Craft.build(player, type)) {
            is CraftOutcome.Built -> players.save(id, outcome.player)
            is CraftOutcome.Refused -> Unit // ressources insuffisantes : aucune écriture.
        }
    }
}
