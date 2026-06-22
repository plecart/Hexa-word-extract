package com.hexa.player

import kotlinx.coroutines.flow.first

/**
 * Récolte paresseuse et **persistance** du résultat (cf. PRD #5, user stories 12-13). Glu mince autour
 * du module pur [HarvestCalculator] : lit l'état (joueur + bâtiments posés), délègue tout le calcul,
 * puis n'écrit **que** si quelque chose a été produit.
 *
 * Déclenchée à l'ouverture de l'app puis toutes les [com.hexa.config.GameConfig.COLLECT_REFRESH_SECONDS]
 * (cf. [PlayerViewModel]) — **sans aucun service ni timer en arrière-plan** : c'est le cycle de vie de
 * l'app qui rythme l'appel. Tout le temps passé app fermée est rattrapé en un seul calcul à la
 * réouverture (le curseur [PlacedBuilding.lastCollectedAt] porte la mémoire de la dernière récolte).
 *
 * Les écritures (crédit d'inventaire via [PlayerRepository.save] en read-modify-write, avance des
 * curseurs via [BuildingsRepository.place]) passent par le SDK Firestore (cache offline) ; au MVP,
 * sans transaction inter-document, elles sont confiées à la synchronisation locale puis distante.
 * L'inventaire crédité remonte alors **seul** à l'UI via [PlayerRepository.observe].
 *
 * @property auth source de l'identité (compte anonyme), pour retrouver l'uid des documents.
 * @property players accès au document joueur (lecture du document + crédit de l'inventaire).
 * @property buildings accès à la sous-collection des bâtiments (lecture + avance des curseurs).
 * @property calculator module pur de calcul de la récolte (horloge + générateur injectés).
 */
class CollectHarvestUseCase(
    private val auth: AuthGateway,
    private val players: PlayerRepository,
    private val buildings: BuildingsRepository,
    private val calculator: HarvestCalculator,
) {
    /**
     * Calcule et persiste la récolte de tous les bâtiments du joueur courant. Sans effet si le joueur
     * n'est pas encore amorcé ou si aucun bâtiment n'a produit d'unité entière depuis sa dernière
     * récolte (cf. [HarvestCalculator]).
     */
    suspend operator fun invoke() {
        val id = auth.ensureSignedIn()
        val player = players.load(id) ?: return // pas encore amorcé : rien à récolter.
        val result = calculator.collect(buildings.observe(id).first())
        if (result.collected.isEmpty()) return // rien produit : aucune écriture inutile.

        players.save(id, player.copy(inventory = player.inventory.plus(result.gains)))
        result.collected.forEach { buildings.place(id, it) }
    }
}
