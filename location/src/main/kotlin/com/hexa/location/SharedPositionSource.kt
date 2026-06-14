package com.hexa.location

import com.hexa.core.geo.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * [PositionSource] qui **multiplexe** un unique flux de positions chaud entre tous ses consommateurs.
 *
 * Les sources concrètes (le GPS réel `FusedLocationSource`) sont *froides* : chaque collecte ouvre un
 * nouvel abonnement GPS. Plusieurs vues consomment pourtant la même position filtrée — caméra de
 * poursuite, grille hexagonale, avatar — ; les brancher directement sur la source froide
 * multiplierait les abonnements GPS et ferait diverger leurs filtres. Ce décorateur partage un seul
 * abonnement amont via [shareIn] : un seul GPS pour toute l'app, arrêté automatiquement quand plus
 * personne n'observe ([SharingStarted.WhileSubscribed]).
 *
 * Le partage vit dans le [scope] fourni (typiquement à l'échelle du processus), pour rester stable au
 * travers des recréations de ViewModels. Le `replay = 1` sert la dernière position aux consommateurs
 * qui s'abonnent en cours de route, sans attendre la prochaine mise à jour GPS.
 *
 * @param delegate source concrète à partager (froide).
 * @param scope portée qui héberge l'abonnement amont partagé.
 * @param stopTimeoutMillis délai de maintien de l'abonnement après la dernière désinscription, pour
 *   absorber les rotations d'écran et navigations brèves sans relancer le GPS.
 */
class SharedPositionSource(
    delegate: PositionSource,
    scope: CoroutineScope,
    stopTimeoutMillis: Long = DEFAULT_STOP_TIMEOUT_MS,
) : PositionSource {
    private val shared: Flow<LatLng> =
        delegate.positions().shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            replay = 1,
        )

    override fun positions(): Flow<LatLng> = shared

    private companion object {
        /** Maintien de 5 s après la dernière désinscription — cohérent avec le ViewModel de caméra. */
        const val DEFAULT_STOP_TIMEOUT_MS = 5_000L
    }
}
