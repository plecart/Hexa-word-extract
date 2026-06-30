package com.hexa.player

import com.google.firebase.Timestamp
import com.hexa.config.Element
import java.time.Instant

/**
 * Traduction entre le modèle pur [Player] et le document Firestore `players/{uid}`.
 *
 * Frontière unique entre le domaine (sans Firebase) et le schéma persisté : les noms de champs et
 * les libellés des compteurs (par élément, par bâtiment) y sont **contractuels** — un futur serveur
 * les relira. Les compteurs absents d'un document sont complétés à zéro à la lecture, pour tolérer
 * l'évolution du schéma (nouvel élément/bâtiment) sans casser les anciens documents.
 *
 * Pur et sans dépendance Android : testable directement (cf. `PlayerDocumentMapperTest`).
 */
object PlayerDocumentMapper {
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_BASE_CELL = "baseCell"
    const val FIELD_INVENTORY = "inventory"
    const val FIELD_BUILT_BUILDINGS = "builtBuildings"

    /** Sérialise [player] vers la forme attendue par Firestore (`set` du document). */
    fun toDocument(player: Player): Map<String, Any?> = mapOf(
        FIELD_CREATED_AT to Timestamp(player.createdAt.epochSecond, player.createdAt.nano),
        FIELD_BASE_CELL to player.baseCell,
        FIELD_INVENTORY to player.inventory.amounts.mapKeys { it.key.fieldKey },
        FIELD_BUILT_BUILDINGS to player.builtBuildings.entries.associate { it.key.fieldKey to it.value.toLong() },
    )

    /**
     * Reconstruit un [Player] depuis les données brutes d'un document Firestore.
     *
     * **Lecture tolérante de bout en bout** (cohérente avec la complétion à zéro des compteurs) : un
     * document légataire, incomplet ou au type inattendu ne plante jamais, il dégrade champ par champ.
     * - `createdAt` absent ou non-`Timestamp` → repli sur [Instant.EPOCH].
     * - `baseCell` absent ou non-`String` → `null` (base réputée non posée).
     *
     * Le repli de `createdAt` sur l'epoch est volontairement neutre : le mapper reste pur (pas
     * d'horloge), et l'aller-retour d'un document écrit par [toDocument] reste exact (il porte toujours
     * un `Timestamp` valide).
     */
    fun fromDocument(data: Map<String, Any?>): Player = Player(
        createdAt = (data[FIELD_CREATED_AT] as? Timestamp)?.toInstant() ?: Instant.EPOCH,
        baseCell = data[FIELD_BASE_CELL] as? String,
        inventory = readInventory(data[FIELD_INVENTORY]),
        builtBuildings = readBuiltBuildings(data[FIELD_BUILT_BUILDINGS]),
    )

    private fun readInventory(raw: Any?): Inventory {
        val counts = raw.asCountMap()
        return Inventory(Element.entries.associateWith { counts.count(it.fieldKey) })
    }

    private fun readBuiltBuildings(raw: Any?): Map<BuildingType, Int> {
        val counts = raw.asCountMap()
        return BuildingType.entries.associateWith { counts.count(it.fieldKey).toInt() }
    }

    /** Instant préservant les nanosecondes (le constructeur `Timestamp(Date)` les perdrait). */
    private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

    private fun Any?.asCountMap(): Map<*, *> = this as? Map<*, *> ?: emptyMap<Any?, Any?>()

    /** Lit un compteur, 0 par défaut s'il est absent (schéma tolérant). */
    private fun Map<*, *>.count(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
}
