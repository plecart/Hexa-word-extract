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
 * Déclenche l'amorçage silencieux du compte au démarrage et **expose l'inventaire chargé** en
 * [StateFlow] observable (cf. PRD #5). Glu mince autour de [EnsurePlayerUseCase] : toute la logique
 * (idempotence, kit de départ) vit dans le domaine, ce qui rend ce ViewModel testable avec des
 * doubles.
 *
 * @param ensurePlayer cas d'usage d'amorçage (compte anonyme + document joueur).
 */
class PlayerViewModel(
    private val ensurePlayer: EnsurePlayerUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)

    /** État courant du compte joueur. */
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value =
                try {
                    val (id, player) = ensurePlayer()
                    PlayerUiState.Ready(id.value, player.inventory)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PlayerUiState.Failed
                }
        }
    }
}
