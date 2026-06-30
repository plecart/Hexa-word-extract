package com.hexa.player

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Adaptateur [AuthGateway] sur Firebase Auth en mode **anonyme**.
 *
 * Réutilise la session persistée par le SDK : au relancement, `currentUser` est déjà là et le même
 * uid est rendu sans recréer de compte — l'idempotence de l'amorçage repose là-dessus. Le passage
 * ultérieur au SSO se fera par account linking sur ce même uid (cf. [PlayerId]).
 *
 * Glue mince autour du SDK (non testée unitairement) ; la logique d'amorçage testable vit dans
 * [EnsurePlayerUseCase].
 */
class FirebaseAuthGateway(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthGateway {
    override suspend fun ensureSignedIn(): PlayerId {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        return PlayerId(requireNotNull(user) { "Connexion anonyme sans utilisateur" }.uid)
    }
}
