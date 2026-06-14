package com.hexa.player

import kotlinx.coroutines.flow.Flow

/**
 * Port d'accès au document joueur `players/{uid}`.
 *
 * Découple le domaine du stockage : cette tranche le câble sur Firestore (cache offline activé)
 * côté `:app` ; les tests utilisent un fake en mémoire.
 */
interface PlayerRepository {
    /** Charge le document du joueur [id], ou `null` s'il n'existe pas encore. */
    suspend fun load(id: PlayerId): Player?

    /** Écrit le document du joueur [id] (création au premier lancement). */
    suspend fun save(id: PlayerId, player: Player)

    /**
     * Observe le document du joueur [id] en continu : émet l'instantané courant, puis ré-émet à
     * **chaque** mise à jour, locale ou distante. C'est ce qui permet à l'inventaire de monter sous
     * les yeux du joueur sans action de sa part (cf. récolte #25).
     *
     * Le flux émet `null` tant que le document n'existe pas. Côté Firestore, le cache offline
     * (activé dans `HexaApplication`) garantit une première émission immédiate puis les écritures
     * locales avant même la synchronisation réseau.
     */
    fun observe(id: PlayerId): Flow<Player?>
}
