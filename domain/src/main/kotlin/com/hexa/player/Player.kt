package com.hexa.player

import com.hexa.config.GameConfig
import java.time.Instant

/**
 * Document joueur, image fidèle de `players/{uid}` dans Firestore (cf. PRD #5).
 *
 * Les noms et la forme des champs sont **contractuels** : ils sont persistés tels quels et un futur
 * serveur les relira. Ce modèle reste pur (aucune dépendance Firebase) ; la traduction vers/depuis
 * Firestore vit côté `:app`.
 *
 * @property createdAt instant de création du compte (horloge client assumée au MVP, anti-triche hors
 *   périmètre) ; posé une seule fois.
 * @property baseCell index H3 de la cellule où la base est posée, ou `null` tant qu'elle ne l'est pas.
 * @property inventory compteurs des cinq éléments.
 * @property builtBuildings stock de bâtiments construits prêts à poser ; couvre **toujours** tous les
 *   [BuildingType] (invariant garanti à la construction), si bien que toute opération de stock
 *   ([stockOf]/[incrementStock]/[decrementStock]) lit n'importe quel type sans gérer l'absence.
 */
data class Player(
    val createdAt: Instant,
    val baseCell: String?,
    val inventory: Inventory,
    val builtBuildings: Map<BuildingType, Int>,
) {
    init {
        require(builtBuildings.keys == BUILDING_TYPES) {
            "Le stock doit couvrir exactement tous les types de bâtiment, reçu : ${builtBuildings.keys}"
        }
    }

    /** Nombre de [type] construits prêts à poser (jamais nul : l'invariant garantit sa présence). */
    fun stockOf(type: BuildingType): Int = builtBuildings.getValue(type)

    /**
     * Document **crédité** d'un [type] fraîchement construit (+1 au stock), p. ex. à l'issue d'un
     * craft réussi. Opération inverse de [decrementStock] ; les autres types sont laissés intacts.
     *
     * @return un nouveau joueur, l'invariant de stock préservé.
     */
    fun incrementStock(type: BuildingType): Player = adjustStock(type, +1)

    /**
     * Document **débité** d'un [type] (−1 au stock), p. ex. quand un bâtiment construit est posé sur la
     * carte. Opération inverse de [incrementStock] ; les autres types sont laissés intacts.
     *
     * Opération de bas niveau : l'appelant garantit un stock disponible (cf. [PlacementRules], qui
     * refuse la pose à stock nul avant de débiter). Sans ce contrôle, le compteur passerait négatif.
     *
     * @return un nouveau joueur, l'invariant de stock préservé.
     */
    fun decrementStock(type: BuildingType): Player = adjustStock(type, -1)

    /** Stock ajusté de [delta] sur [type] (les autres types intacts), l'invariant de stock préservé. */
    private fun adjustStock(type: BuildingType, delta: Int): Player =
        copy(builtBuildings = builtBuildings + (type to stockOf(type) + delta))

    companion object {
        private val BUILDING_TYPES: Set<BuildingType> = BuildingType.entries.toSet()

        /**
         * Amorce un nouveau joueur au premier lancement : kit de départ lu depuis
         * [GameConfig.STARTER_KIT], aucune base posée, stock de bâtiments à zéro.
         *
         * @param createdAt instant de création (fourni par l'horloge de l'appelant).
         */
        fun newPlayer(createdAt: Instant): Player = Player(
            createdAt = createdAt,
            baseCell = null,
            inventory = Inventory.of(GameConfig.STARTER_KIT),
            builtBuildings = BuildingType.entries.associateWith { 0 },
        )
    }
}
