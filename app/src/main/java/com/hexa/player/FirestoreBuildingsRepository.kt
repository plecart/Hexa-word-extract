package com.hexa.player

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Adaptateur [BuildingsRepository] sur Firestore (`players/{uid}/buildings/{h3Index}`).
 *
 * L'index H3 de la tuile sert d'identifiant de document : `set` crée ou écrase, ce qui réalise la
 * règle « un bâtiment par tuile ». Le cache offline (cf. [com.hexa.HexaApplication]) prend l'écriture
 * en charge sans réseau puis la synchronise. La (sé)rialisation est déléguée à
 * [BuildingDocumentMapper], qui porte le contrat de schéma.
 *
 * Glue mince autour du SDK (non testée unitairement) ; le mapper et la pose sont testés à part.
 */
class FirestoreBuildingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : BuildingsRepository {
    override suspend fun place(id: PlayerId, building: PlacedBuilding) {
        document(id, building.cell).set(BuildingDocumentMapper.toDocument(building)).await()
    }

    private fun document(id: PlayerId, cell: String) = firestore
        .collection(COLLECTION_PLAYERS)
        .document(id.value)
        .collection(COLLECTION_BUILDINGS)
        .document(cell)

    private companion object {
        const val COLLECTION_PLAYERS = "players"
        const val COLLECTION_BUILDINGS = "buildings"
    }
}
