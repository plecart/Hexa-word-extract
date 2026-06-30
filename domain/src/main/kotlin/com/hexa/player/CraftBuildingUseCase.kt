package com.hexa.player

/**
 * Craft d'un bâtiment et **persistance** du résultat (cf. PRD #5, user stories 8-11).
 *
 * Glu mince autour du module pur [Craft] : charge le document joueur, délègue la décision (suffisance,
 * débit, crédit), n'écrit que sur un succès, et **renvoie le verdict** à l'appelant. Un craft refusé
 * **ne touche pas Firestore** et remonte ses éléments manquants : c'est la couture de validation que
 * l'UI exploite pour motiver le refus, et qu'un futur backend autoritaire devra ré-appliquer (cf. #137).
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
     * Le verdict est **renvoyé** à l'appelant — pas avalé : un [CraftOutcome.Refused] expose les
     * éléments manquants pour que l'UI les motive, et constitue la couture de validation qu'un futur
     * backend autoritaire devra ré-appliquer (cf. #137). Seul un [CraftOutcome.Built] écrit ; le débit
     * et le crédit remontent ensuite par l'observation du document.
     *
     * @param type bâtiment à construire (détermine la recette, cf. [Craft]).
     * @return [CraftOutcome.Built] (document persisté) si les ressources suffisent, sinon
     *   [CraftOutcome.Refused] avec le détail des manquants — aucune écriture dans ce cas.
     * @throws IllegalStateException si le document joueur n'existe pas encore (amorçage requis avant
     *   tout craft, cf. [EnsurePlayerUseCase]).
     */
    suspend operator fun invoke(type: BuildingType): CraftOutcome {
        val id = auth.ensureSignedIn()
        val player =
            players.load(id)
                ?: error("Craft impossible : document joueur absent (amorçage requis).")
        val outcome = Craft.build(player, type)
        if (outcome is CraftOutcome.Built) players.save(id, outcome.player)
        return outcome
    }
}
