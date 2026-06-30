package com.hexa.player

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /**
     * Pont entre l'écouteur d'instantanés Firestore et un [Flow]. `addSnapshotListener` émet
     * immédiatement l'instantané en cache (offline), puis ré-émet à chaque écriture locale et à
     * chaque synchronisation distante ; l'écouteur est retiré quand le flux n'est plus collecté.
     */
    override fun observe(id: PlayerId): Flow<Player?> = callbackFlow {
        val registration = document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else {
                trySend(snapshot?.data?.let(PlayerDocumentMapper::fromDocument))
            }
        }
        awaitClose { registration.remove() }
    }

    private fun document(id: PlayerId) = FirestorePaths.player(firestore, id)
}
