package com.hexa.player

/**
 * Port d'accès au document joueur `players/{uid}`.
 *
 * Découple le domaine du stockage : cette tranche le câble sur Firestore (cache offline activé)
 * côté `:app` ; les tests utilisent un fake en mémoire. Volontairement minimal — lecture ponctuelle
 * et création — pour ne porter que ce dont l'amorçage a besoin.
 */
interface PlayerRepository {
    /** Charge le document du joueur [id], ou `null` s'il n'existe pas encore. */
    suspend fun load(id: PlayerId): Player?

    /** Écrit le document du joueur [id] (création au premier lancement). */
    suspend fun save(id: PlayerId, player: Player)
}
