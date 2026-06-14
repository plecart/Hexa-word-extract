package com.hexa.player

/**
 * Port d'authentification : découple le domaine du fournisseur d'identité.
 *
 * Cette tranche le câble sur Firebase Auth anonyme (côté `:app`) ; un SSO pourra le remplacer plus
 * tard par account linking sans toucher au domaine — l'uid reste stable (cf. [PlayerId]).
 */
fun interface AuthGateway {
    /**
     * Garantit une session authentifiée et retourne l'uid courant. Idempotent : à un lancement
     * ultérieur, retourne l'uid existant sans recréer de compte.
     */
    suspend fun ensureSignedIn(): PlayerId
}
