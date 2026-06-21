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

    /**
     * Compte prêt : uid, inventaire, stock de bâtiments construits prêts à poser, et cellule de la
     * base ([baseCell] `null` tant qu'elle n'est pas posée — c'est ce qui décide l'affichage de
     * l'écran de premier lancement, cf. PRD #5).
     */
    data class Ready(
        val uid: String,
        val inventory: Inventory,
        val builtBuildings: Map<BuildingType, Int>,
        val baseCell: String?,
    ) : PlayerUiState

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
 * (récolte) ou distante (autre appareil) — se reflète dans [state] sans action de l'utilisateur.
 *
 * `Failed` est réservé à l'**échec d'amorçage** (compte/document indisponible au démarrage). Une fois
 * l'inventaire affiché, un incident du flux temps réel ne le fait pas régresser : les émissions
 * `null` (document transitoirement absent) et une éventuelle erreur du flux conservent le dernier
 * inventaire connu.
 *
 * @param ensurePlayer cas d'usage d'amorçage (compte anonyme + document joueur).
 * @param repository port d'observation du document joueur.
 * @param buildings port d'observation de la sous-collection des bâtiments posés.
 * @param craftBuilding cas d'usage de craft d'un bâtiment (débit inventaire + crédit stock, persisté).
 */
class PlayerViewModel(
    private val ensurePlayer: EnsurePlayerUseCase,
    private val repository: PlayerRepository,
    private val buildings: BuildingsRepository,
    private val craftBuilding: CraftBuildingUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)

    /** État courant du compte joueur. */
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _builtCells = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Index H3 des cellules **bâties** du joueur, pour le rendu de la grille (cf.
     * [com.hexa.map.HexGridViewModel]). Dérivé de la **sous-collection des bâtiments** ([buildings]) :
     * toute pose — la base ou un futur extracteur — fait apparaître la tuile « bâtie » sans recréer la
     * carte. Vide tant qu'aucun bâtiment n'est posé.
     */
    val builtCells: StateFlow<Set<String>> = _builtCells.asStateFlow()

    init {
        viewModelScope.launch {
            val (id, player) =
                try {
                    ensurePlayer()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value = PlayerUiState.Failed
                    return@launch
                }

            _state.value = player.toReady(id)
            launch { observeBuildings(id) }
            observePlayer(id)
        }
    }

    /**
     * Construit un extracteur : débite l'inventaire, crédite le stock, persiste. Le résultat **remonte
     * seul** par l'observation du document ([observePlayer]) — pas de mutation directe de [state] ici.
     * Un échec (réseau, ressources insuffisantes en cas de course) ne fait pas régresser l'affichage.
     */
    fun craftExtracteur() {
        viewModelScope.launch {
            try {
                craftBuilding(BuildingType.EXTRACTEUR)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Le craft a échoué : on conserve le dernier état connu, sans régression.
            }
        }
    }

    /**
     * Reflète la sous-collection des bâtiments dans [builtCells] (index H3 des tuiles bâties). Un
     * incident du flux conserve le dernier ensemble connu, sans régression de l'affichage.
     */
    private suspend fun observeBuildings(id: PlayerId) {
        try {
            buildings.observe(id).collect { placed ->
                _builtCells.value = placed.mapTo(mutableSetOf()) { it.cell }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // L'observation des bâtiments a échoué : on conserve le dernier ensemble connu.
        }
    }

    /** Reflète chaque mise à jour du document dans [state] ; un incident du flux n'efface rien. */
    private suspend fun observePlayer(id: PlayerId) {
        try {
            repository.observe(id).collect { live ->
                if (live != null) {
                    _state.value = live.toReady(id)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Le live a échoué après l'affichage : on conserve le dernier état connu.
        }
    }

    /** Projette le document joueur en état `Ready` affichable (inventaire + stock + base). */
    private fun Player.toReady(id: PlayerId): PlayerUiState.Ready =
        PlayerUiState.Ready(id.value, inventory, builtBuildings, baseCell)
}
