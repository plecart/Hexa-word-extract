package com.hexa.player

import java.time.Clock

/**
 * Pose d'un **extracteur** depuis le stock construit (cf. PRD #4, user stories 5-9 ; spec F5).
 *
 * Pendant en écriture de [PlaceBaseUseCase], mais payant : sur la tuile [cell] (index H3 de la tuile
 * courante, déjà résolu côté `:app`), la pose **décrémente** `builtBuildings[EXTRACTEUR]` et crée le
 * document `players/{uid}/buildings/{h3Index}` de type `extracteur`, `lastCollectedAt = now` pour
 * amorcer la récolte. C'est l'opération inverse du craft ([Craft]) sur la même map de stock.
 *
 * La décision est déléguée à [PlacementRules] et **rien n'est écrit en cas de refus** (calque de
 * [CraftBuildingUseCase] : décider d'abord, persister seulement si succès). L'affordance n'étant
 * offerte que sur la tuile courante, ce cas d'usage revalide uniquement les **invariants de données**
 * — occupation (lue via [BuildingsRepository.building], qui réalise « un bâtiment par tuile » sans
 * écraser) et stock — en tenant la tuile courante pour acquise.
 *
 * @property auth source de l'identité (compte anonyme), pour retrouver l'uid du document joueur.
 * @property players accès au document joueur (stock `builtBuildings`).
 * @property buildings sous-collection des bâtiments posés (lecture d'occupation + écriture).
 * @property clock horloge fournissant l'instant de pose (`placedAt`/`lastCollectedAt`), injectée pour
 *   la testabilité.
 */
class PlaceExtractorUseCase(
    private val auth: AuthGateway,
    private val players: PlayerRepository,
    private val buildings: BuildingsRepository,
    private val clock: Clock,
) {
    /**
     * Pose un extracteur sur la tuile [cell].
     *
     * @param cell index H3 de la tuile courante du joueur (= ID du document bâtiment).
     * @return la [PlacementDecision] : [PlacementDecision.Placeable] si la pose a eu lieu (stock
     *   décrémenté, document créé), sinon [PlacementDecision.Refused] avec sa raison — aucune écriture.
     * @throws IllegalStateException si le document joueur n'existe pas encore (amorçage requis).
     */
    suspend operator fun invoke(cell: String): PlacementDecision {
        val id = auth.ensureSignedIn()
        val player =
            players.load(id)
                ?: error("Pose d'un extracteur impossible : document joueur absent (amorçage requis).")
        val stock = player.builtBuildings.getValue(BuildingType.EXTRACTEUR)
        val occupied = buildings.building(id, cell) != null

        val decision = PlacementRules.decide(isCurrentTile = true, isTileOccupied = occupied, stock = stock)
        if (decision is PlacementDecision.Placeable) {
            val decremented = player.builtBuildings + (BuildingType.EXTRACTEUR to stock - 1)
            players.save(id, player.copy(builtBuildings = decremented))
            buildings.place(id, PlacedBuilding.extracteur(cell, clock.instant()))
        }
        return decision
    }
}
