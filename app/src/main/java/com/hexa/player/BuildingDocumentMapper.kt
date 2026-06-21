package com.hexa.player

import com.google.firebase.Timestamp
import java.time.Instant

/**
 * Traduction du modèle pur [PlacedBuilding] vers le document Firestore
 * `players/{uid}/buildings/{h3Index}` (cf. spec F5).
 *
 * Frontière entre le domaine (sans Firebase) et le schéma persisté : le libellé du type et les noms
 * de champs y sont **contractuels** — un futur serveur les relira. L'index H3 de la tuile n'est pas
 * un champ : il sert d'**identifiant de document** (posé par [FirestoreBuildingsRepository]), ce qui
 * garantit la règle « un bâtiment par tuile ».
 *
 * Seule l'écriture est traduite : la lecture des bâtiments (rendu, récolte) n'existe pas encore et
 * sa désérialisation viendra avec elle. Pur et sans dépendance Android : testable directement.
 */
object BuildingDocumentMapper {
    const val FIELD_TYPE = "type"
    const val FIELD_PLACED_AT = "placedAt"
    const val FIELD_LAST_COLLECTED_AT = "lastCollectedAt"

    /** Sérialise [building] vers la forme attendue par Firestore (`set` du document). */
    fun toDocument(building: PlacedBuilding): Map<String, Any?> = mapOf(
        FIELD_TYPE to building.type.fieldKey,
        FIELD_PLACED_AT to building.placedAt.toTimestamp(),
        FIELD_LAST_COLLECTED_AT to building.lastCollectedAt.toTimestamp(),
    )

    /** Timestamp Firestore préservant les nanosecondes (le constructeur `Timestamp(Date)` les perdrait). */
    private fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

    private val PlacedBuildingType.fieldKey: String get() = name.lowercase()
}
