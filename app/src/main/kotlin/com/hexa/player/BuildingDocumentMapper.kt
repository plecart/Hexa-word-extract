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
 * Écriture et lecture sont symétriques ([toDocument] / [fromDocument]) : un aller-retour restitue le
 * bâtiment d'origine, nanosecondes comprises. Pur et sans dépendance Android : testable directement.
 *
 * **Lecture tolérante** (cohérente avec celle du document joueur, cf. [PlayerDocumentMapper]) : appelé
 * dans le flux temps réel qui alimente le rendu, [fromDocument] ne **lève jamais**. Un document
 * malformé, incomplet ou légataire (type renommé/supprimé d'une version future, écriture offline
 * partielle) est **écarté** (`null`), si bien que les autres bâtiments restent rendus. Contrairement au
 * document joueur — qui dégrade champ par champ vers des valeurs neutres — un bâtiment n'a pas de repli
 * sûr par champ : un type inconnu n'a pas de modèle à afficher, et un `lastCollectedAt` replié sur
 * l'epoch créditerait une récolte démesurée. L'unité de tolérance est donc le **document entier**.
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

    /**
     * Désérialise les [data] d'un document `buildings/{h3Index}` en [PlacedBuilding], ou `null` si le
     * document est illisible (champ requis absent, de mauvais type, ou type de bâtiment inconnu). L'index
     * H3 n'est pas un champ : il est fourni par l'appelant via [cell] (= identifiant du document Firestore).
     */
    fun fromDocument(cell: String, data: Map<String, Any?>): PlacedBuilding? {
        val type = (data[FIELD_TYPE] as? String)?.toEnumOrNull<PlacedBuildingType>() ?: return null
        val placedAt = (data[FIELD_PLACED_AT] as? Timestamp)?.toInstant() ?: return null
        val lastCollectedAt = (data[FIELD_LAST_COLLECTED_AT] as? Timestamp)?.toInstant() ?: return null
        return PlacedBuilding(cell, type, placedAt, lastCollectedAt)
    }

    /** Timestamp Firestore préservant les nanosecondes (le constructeur `Timestamp(Date)` les perdrait). */
    private fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

    /** Instant reconstruit depuis le Timestamp Firestore, nanosecondes comprises. */
    private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
}
