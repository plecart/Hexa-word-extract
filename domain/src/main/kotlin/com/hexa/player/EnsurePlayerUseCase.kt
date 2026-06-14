package com.hexa.player

import java.time.Clock

/**
 * Joueur identifié : l'uid et son document, retournés ensemble par [EnsurePlayerUseCase] pour éviter
 * une relecture côté appelant.
 */
data class IdentifiedPlayer(val id: PlayerId, val player: Player)

/**
 * Amorçage silencieux du compte au démarrage (cf. PRD #5, user stories 1, 2, 17).
 *
 * Garantit la session anonyme puis le document joueur :
 * - **premier lancement** : aucun document → en crée un avec le kit de départ ;
 * - **lancements suivants** : le document existe → le réutilise tel quel, **sans réécriture** (le
 *   kit n'est jamais re-crédité).
 *
 * L'horloge est injectée pour fixer `createdAt` de façon testable.
 *
 * @property auth source de l'identité (compte anonyme).
 * @property repository accès au document joueur.
 * @property clock horloge fournissant l'instant de création d'un nouveau document.
 */
class EnsurePlayerUseCase(
    private val auth: AuthGateway,
    private val repository: PlayerRepository,
    private val clock: Clock,
) {
    /** @return l'uid et le document joueur (existant ou fraîchement créé). */
    suspend operator fun invoke(): IdentifiedPlayer {
        val id = auth.ensureSignedIn()
        val player =
            repository.load(id)
                ?: Player.newPlayer(clock.instant()).also { repository.save(id, it) }
        return IdentifiedPlayer(id, player)
    }
}
