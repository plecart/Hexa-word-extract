package com.hexa.player

import java.time.Clock

/**
 * Pose d'un **extracteur** depuis le stock construit (cf. PRD #4, user stories 5-9 ; spec F5).
 *
 * Pendant en écriture de [PlaceBaseUseCase], mais payant : sur la tuile [cell] (index H3 de la tuile
 * courante, déjà résolu côté `:app`), la pose **décrémente** le stock d'extracteur
 * ([Player.decrementStock]) et crée le document `players/{uid}/buildings/{h3Index}` de type
 * `extracteur`, `lastCollectedAt = now` pour amorcer la récolte. C'est l'opération inverse du craft
 * ([Craft]), qui l'incrémente.
 *
 * La décision est déléguée à [PlacementRules] et **rien n'est écrit en cas de refus** (calque de
 * [CraftBuildingUseCase] : décider d'abord, persister seulement si succès). Les **trois** conditions
 * sont réellement évaluées : la tuile visée est comparée à la **tuile courante réelle** du joueur
 * ([currentTile]) — le use case ne fait pas confiance à l'appelant pour cette garde —, l'occupation
 * est lue via [BuildingsRepository.building] (qui réalise « un bâtiment par tuile » sans écraser), et
 * le stock via le document. C'est la couture de sécurité qu'un futur backend autoritaire ré-appliquera
 * (cf. #137).
 *
 * @property auth source de l'identité (compte anonyme), pour retrouver l'uid du document joueur.
 * @property players accès au document joueur (stock `builtBuildings`).
 * @property buildings sous-collection des bâtiments posés (lecture d'occupation + écriture).
 * @property currentTile tuile courante réelle du joueur, pour valider qu'on pose bien « sous ses pieds »
 *   (index H3 textuel, cf. [CurrentTileGateway]).
 * @property clock horloge fournissant l'instant de pose (`placedAt`/`lastCollectedAt`), injectée pour
 *   la testabilité.
 */
class PlaceExtractorUseCase(
    private val auth: AuthGateway,
    private val players: PlayerRepository,
    private val buildings: BuildingsRepository,
    private val currentTile: CurrentTileGateway,
    private val clock: Clock,
) {
    /**
     * Pose un extracteur sur la tuile [cell].
     *
     * @param cell index H3 de la tuile visée (= ID du document bâtiment). Refusé
     *   ([PlacementRefusal.NOT_CURRENT_TILE]) s'il ne coïncide pas avec la tuile courante réelle du
     *   joueur ([currentTile]).
     * @return la [PlacementDecision] : [PlacementDecision.Placeable] si la pose a eu lieu (stock
     *   décrémenté, document créé), sinon [PlacementDecision.Refused] avec sa raison — aucune écriture.
     * @throws IllegalStateException si le document joueur n'existe pas encore (amorçage requis).
     */
    suspend operator fun invoke(cell: String): PlacementDecision {
        val id = auth.ensureSignedIn()
        val player =
            players.load(id)
                ?: error("Pose d'un extracteur impossible : document joueur absent (amorçage requis).")
        val stock = player.stockOf(BuildingType.EXTRACTEUR)
        val occupied = buildings.building(id, cell) != null

        val decision = PlacementRules.decide(
            isCurrentTile = cell == currentTile.currentCell(),
            isTileOccupied = occupied,
            stock = stock,
        )
        if (decision is PlacementDecision.Placeable) {
            players.save(id, player.decrementStock(BuildingType.EXTRACTEUR))
            buildings.place(id, PlacedBuilding.extracteur(cell, clock.instant()))
        }
        return decision
    }
}
