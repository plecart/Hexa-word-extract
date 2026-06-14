package com.hexa.player

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Adaptateur [PlayerRepository] sur Firestore (`players/{uid}`).
 *
 * Le cache offline est activé au démarrage (cf. [com.hexa.HexaApplication]) : après un premier
 * lancement réussi, [load] sert le document depuis le cache local sans réseau. La (dé)sérialisation
 * est déléguée à [PlayerDocumentMapper], qui porte le contrat de schéma.
 *
 * Glue mince autour du SDK (non testée unitairement) ; le mapper et l'amorçage sont testés à part.
 */
class FirestorePlayerRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PlayerRepository {
    override suspend fun load(id: PlayerId): Player? =
        document(id).get().await().data?.let(PlayerDocumentMapper::fromDocument)

    override suspend fun save(id: PlayerId, player: Player) {
        document(id).set(PlayerDocumentMapper.toDocument(player)).await()
    }

    private fun document(id: PlayerId) = firestore.collection(COLLECTION_PLAYERS).document(id.value)

    private companion object {
        const val COLLECTION_PLAYERS = "players"
    }
}
