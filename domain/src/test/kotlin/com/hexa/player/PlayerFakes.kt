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

/**
 * [BuildingsRepository] adossé à une carte mutable, indexée par tuile (un doc par index H3, comme
 * Firestore). Enregistre les écritures dans [saved] pour vérifier l'absence de débit/pose superflue.
 */
class FakeBuildingsRepository : BuildingsRepository {
    private val documents = mutableMapOf<PlayerId, MutableMap<String, PlacedBuilding>>()
    private val streams = mutableMapOf<PlayerId, MutableStateFlow<List<PlacedBuilding>>>()
    val saved = mutableListOf<Pair<PlayerId, PlacedBuilding>>()

    override suspend fun place(id: PlayerId, building: PlacedBuilding) {
        val byCell = documents.getOrPut(id) { mutableMapOf() }
        byCell[building.cell] = building
        saved += id to building
        streamOf(id).value = byCell.values.toList()
    }

    override fun observe(id: PlayerId): Flow<List<PlacedBuilding>> = streamOf(id).asStateFlow()

    /** Bâtiments posés par le joueur [id], indexés par tuile, pour les assertions. */
    fun buildingsOf(id: PlayerId): Map<String, PlacedBuilding> = documents[id] ?: emptyMap()

    /** Flux par joueur, amorcé sur les documents courants — comme un instantané Firestore initial. */
    private fun streamOf(id: PlayerId): MutableStateFlow<List<PlacedBuilding>> =
        streams.getOrPut(id) { MutableStateFlow(documents[id]?.values?.toList().orEmpty()) }
}
