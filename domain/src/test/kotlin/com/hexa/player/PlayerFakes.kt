package com.hexa.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fakes en mémoire des ports joueur, partagés par les tests d'amorçage. Ils remplacent Firebase :
 * le domaine étant pur, aucun émulateur n'est requis pour vérifier la logique.
 */

/** [AuthGateway] qui retourne toujours le même uid et compte les appels. */
class FakeAuthGateway(private val id: PlayerId) : AuthGateway {
    var signInCount = 0
        private set

    override suspend fun ensureSignedIn(): PlayerId {
        signInCount++
        return id
    }
}

/**
 * [PlayerRepository] adossé à une carte mutable. Enregistre les écritures dans [saved] pour vérifier
 * l'idempotence (aucune écriture au relancement).
 */
class FakePlayerRepository(initial: Map<PlayerId, Player> = emptyMap()) : PlayerRepository {
    private val documents = initial.toMutableMap()
    private val streams = mutableMapOf<PlayerId, MutableStateFlow<Player?>>()
    val saved = mutableListOf<Pair<PlayerId, Player>>()

    override suspend fun load(id: PlayerId): Player? = documents[id]

    override suspend fun save(id: PlayerId, player: Player) {
        documents[id] = player
        saved += id to player
        streamOf(id).value = player
    }

    override fun observe(id: PlayerId): Flow<Player?> = streamOf(id).asStateFlow()

    /** État courant du document [id], pour les assertions. */
    fun stored(id: PlayerId): Player? = documents[id]

    /** Flux par joueur, amorcé sur le document courant — comme un instantané Firestore initial. */
    private fun streamOf(id: PlayerId): MutableStateFlow<Player?> =
        streams.getOrPut(id) { MutableStateFlow(documents[id]) }
}
