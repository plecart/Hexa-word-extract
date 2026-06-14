package com.hexa.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * État observable du compte joueur, exposé à l'UI.
 */
sealed interface PlayerUiState {
    /** Amorçage en cours (premier état). */
    data object Loading : PlayerUiState

    /** Compte prêt : uid et inventaire chargés. */
    data class Ready(val uid: String, val inventory: Inventory) : PlayerUiState

    /** L'amorçage a échoué (ex. réseau indisponible au tout premier lancement). */
    data object Failed : PlayerUiState
}

/**
 * Amorce le compte au démarrage puis **observe le document joueur en continu**, exposant l'inventaire
 * en [StateFlow] (cf. PRD #5). Glu mince : la logique d'amorçage (idempotence, kit de départ) vit dans
 * [EnsurePlayerUseCase] et l'observation dans [PlayerRepository], ce qui rend ce ViewModel testable
 * avec des doubles.
 *
 * Après l'amorçage, [PlayerRepository.observe] alimente l'inventaire : toute écriture — locale
 * (récolte) ou distante (autre appareil) — se reflète dans [state] sans action de l'utilisateur. Les
 * émissions `null` (document transitoirement absent) sont ignorées pour conserver le dernier
 * inventaire connu plutôt que de régresser.
 *
 * @param ensurePlayer cas d'usage d'amorçage (compte anonyme + document joueur).
 * @param repository port d'observation du document joueur.
 */
class PlayerViewModel(
    private val ensurePlayer: EnsurePlayerUseCase,
    private val repository: PlayerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)

    /** État courant du compte joueur. */
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val (id, player) = ensurePlayer()
                _state.value = PlayerUiState.Ready(id.value, player.inventory)
                repository.observe(id).collect { live ->
                    if (live != null) {
                        _state.value = PlayerUiState.Ready(id.value, live.inventory)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PlayerUiState.Failed
            }
        }
    }
}
