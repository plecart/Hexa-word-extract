package com.hexa.player

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Adaptateur [BuildingsRepository] sur Firestore (`players/{uid}/buildings/{h3Index}`).
 *
 * L'index H3 de la tuile sert d'identifiant de document : `set` crée ou écrase, ce qui réalise la
 * règle « un bâtiment par tuile ». Le cache offline (cf. [com.hexa.HexaApplication]) prend l'écriture
 * en charge sans réseau puis la synchronise. La (dé)sérialisation est déléguée à
 * [BuildingDocumentMapper], qui porte le contrat de schéma.
 *
 * Glue mince autour du SDK (non testée unitairement) ; le mapper et la pose sont testés à part.
 */
class FirestoreBuildingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : BuildingsRepository {
    override suspend fun place(id: PlayerId, building: PlacedBuilding) {
        buildings(id).document(building.cell).set(BuildingDocumentMapper.toDocument(building)).await()
    }

    /**
     * Pont entre l'écouteur d'instantanés de la sous-collection et un [Flow] (cf.
     * [FirestorePlayerRepository.observe]). `addSnapshotListener` émet l'instantané en cache (offline)
     * puis ré-émet à chaque pose locale et synchronisation distante ; chaque document est désérialisé
     * sous son index H3 (= id du document). L'écouteur est retiré quand le flux n'est plus collecté.
     */
    override fun observe(id: PlayerId): Flow<List<PlacedBuilding>> = callbackFlow {
        val registration = buildings(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else {
                trySend(
                    snapshot?.documents.orEmpty().mapNotNull { doc ->
                        doc.data?.let { BuildingDocumentMapper.fromDocument(doc.id, it) }
                    },
                )
            }
        }
        awaitClose { registration.remove() }
    }

    private fun buildings(id: PlayerId) = firestore
        .collection(COLLECTION_PLAYERS)
        .document(id.value)
        .collection(COLLECTION_BUILDINGS)

    private companion object {
        const val COLLECTION_PLAYERS = "players"
        const val COLLECTION_BUILDINGS = "buildings"
    }
}
