package com.hexa.player

import java.time.Clock

/**
 * Pose de la **base offerte** — le premier geste du jeu (cf. PRD #5, user stories 3-5 ; spec F5).
 *
 * Sur la tuile [cell] (index H3 choisi par le joueur, déjà résolu côté `:app`), la pose :
 * - renseigne `baseCell` sur le document joueur, **sans rien débiter** : la base est gratuite, ni
 *   l'inventaire ni le stock de bâtiments construits ne bougent ;
 * - crée le document `players/{uid}/buildings/{h3Index}` de type `base`, avec `lastCollectedAt = now`
 *   pour amorcer le futur calcul de récolte.
 *
 * Idempotence du **flux** : l'écran de premier lancement n'appelle ce cas d'usage que tant que
 * `baseCell` est nul (cf. PR2), si bien qu'une base déjà posée n'est jamais réécrite.
 *
 * Les deux écritures passent par le SDK Firestore (cache offline) ; au MVP, sans transaction
 * inter-document, elles sont confiées à la synchronisation locale puis distante.
 *
 * @property auth source de l'identité (compte anonyme), pour retrouver l'uid du document joueur.
 * @property players accès au document joueur (`baseCell`).
 * @property buildings écriture de la sous-collection des bâtiments placés.
 * @property clock horloge fournissant l'instant de pose (`placedAt`/`lastCollectedAt`), injectée
 *   pour la testabilité.
 */
class PlaceBaseUseCase(
    private val auth: AuthGateway,
    private val players: PlayerRepository,
    private val buildings: BuildingsRepository,
    private val clock: Clock,
) {
    /**
     * Pose la base sur la tuile [cell].
     *
     * @param cell index H3 de la tuile courante du joueur (= ID du document bâtiment).
     * @throws IllegalStateException si le document joueur n'existe pas encore (amorçage requis avant
     *   la pose, cf. [EnsurePlayerUseCase]).
     */
    suspend operator fun invoke(cell: String) {
        val id = auth.ensureSignedIn()
        val player =
            players.load(id)
                ?: error("Pose de la base impossible : document joueur absent (amorçage requis).")
        players.save(id, player.copy(baseCell = cell))
        buildings.place(id, PlacedBuilding.base(cell, clock.instant()))
    }
}
