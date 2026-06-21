package com.hexa.player

/**
 * Port d'écriture de la sous-collection `players/{uid}/buildings/{h3Index}` (cf. spec F5).
 *
 * Découple le domaine du stockage : cette tranche le câble sur Firestore côté `:app` ; les tests
 * utilisent un fake en mémoire. La lecture des bâtiments (pour le rendu et la récolte) viendra avec
 * les tranches qui en ont besoin — ce port ne porte que la pose.
 */
interface BuildingsRepository {
    /**
     * Écrit le document du bâtiment [building] pour le joueur [id], sous l'index H3 de sa tuile.
     * L'identifiant de document étant la tuile, re-poser sur la même tuile écrase le document.
     */
    suspend fun place(id: PlayerId, building: PlacedBuilding)
}
