package com.hexa.player

import kotlinx.coroutines.flow.Flow

/**
 * Port de la sous-collection `players/{uid}/buildings/{h3Index}` (cf. spec F5).
 *
 * Découple le domaine du stockage : cette tranche le câble sur Firestore côté `:app` ; les tests
 * utilisent un fake en mémoire.
 */
interface BuildingsRepository {
    /**
     * Écrit le document du bâtiment [building] pour le joueur [id], sous l'index H3 de sa tuile.
     * L'identifiant de document étant la tuile, re-poser sur la même tuile écrase le document.
     */
    suspend fun place(id: PlayerId, building: PlacedBuilding)

    /**
     * Observe en continu les bâtiments posés par le joueur [id]. Émet la liste complète à chaque
     * écriture locale (pose) et à chaque synchronisation distante ; **source unique** pour le rendu 3D
     * des bâtiments et l'état « bâtie » des tuiles de la grille. L'ordre des bâtiments n'est pas garanti.
     */
    fun observe(id: PlayerId): Flow<List<PlacedBuilding>>
}
