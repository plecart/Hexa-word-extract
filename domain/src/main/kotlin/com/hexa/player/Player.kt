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
 * @property builtBuildings stock de bâtiments construits prêts à poser, par type.
 */
data class Player(
    val createdAt: Instant,
    val baseCell: String?,
    val inventory: Inventory,
    val builtBuildings: Map<BuildingType, Int>,
) {
    companion object {
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
