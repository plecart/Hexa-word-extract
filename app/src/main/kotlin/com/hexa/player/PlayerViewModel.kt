package com.hexa.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
 * La **récolte paresseuse** (cf. PRD #5, user stories 12-13) est déclenchée sur [harvestTicks] : à
 * chaque émission, [collectHarvest] règle les comptes des bâtiments et crédite l'inventaire — le
 * crédit remonte ensuite seul par l'observation du document. La cadence est **injectée** (un tick à
 * l'ouverture puis toutes les [com.hexa.config.GameConfig.COLLECT_REFRESH_SECONDS] en production) :
 * aucun timer en arrière-plan, et la récolte reste pilotable en test par un flux fini.
 *
 * @param ensurePlayer cas d'usage d'amorçage (compte anonyme + document joueur).
 * @param repository port d'observation du document joueur.
 * @param buildings port d'observation de la sous-collection des bâtiments posés.
 * @param craftBuilding cas d'usage de craft d'un bâtiment (débit inventaire + crédit stock, persisté).
 * @param placeExtractor cas d'usage de pose d'un extracteur (décrément du stock + écriture du
 *   bâtiment, persisté), opération inverse du craft sur la même map de stock.
 * @param collectHarvest cas d'usage de récolte (crédit d'inventaire + avance des curseurs, persisté).
 * @param harvestTicks cadence de récolte : une récolte par émission ; vide par défaut (aucune récolte).
 */
class PlayerViewModel(
    private val ensurePlayer: EnsurePlayerUseCase,
    private val repository: PlayerRepository,
    private val buildings: BuildingsRepository,
    private val craftBuilding: CraftBuildingUseCase,
    private val placeExtractor: PlaceExtractorUseCase,
    private val collectHarvest: CollectHarvestUseCase,
    private val harvestTicks: Flow<Unit> = emptyFlow(),
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)

    /** État courant du compte joueur. */
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _placedBuildings = MutableStateFlow<List<PlacedBuilding>>(emptyList())

    /**
     * Bâtiments **posés** du joueur, observés depuis la sous-collection ([buildings]) — **source
     * unique** du rendu 3D sur la carte (cf. [com.hexa.map.buildingPlacements]). Toute pose — la base
     * ou un futur extracteur — s'y reflète sans recréer la carte. Vide tant qu'aucun n'est posé.
     */
    val placedBuildings: StateFlow<List<PlacedBuilding>> = _placedBuildings.asStateFlow()

    /**
     * Nombre d'extracteurs **construits prêts à poser** (`builtBuildings[EXTRACTEUR]`), projeté depuis
     * [state] : pilote l'apparition du marqueur « + » sur la carte (cf. [com.hexa.map.extractorPlacementCell]).
     * `0` tant que l'état n'est pas `Ready`.
     */
    val extractorStock: StateFlow<Int> =
        state
            .map { (it as? PlayerUiState.Ready)?.builtBuildings?.get(BuildingType.EXTRACTEUR) ?: 0 }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

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
            launch { harvestOnTicks() }
            observePlayer(id)
        }
    }

    /**
     * Récolte à chaque émission de [harvestTicks] (ouverture puis cadence périodique). L'échec d'une
     * passe (réseau, course) est avalé : on réessaiera au tick suivant, sans faire régresser l'affichage.
     */
    private suspend fun harvestOnTicks() {
        harvestTicks.collect {
            try {
                collectHarvest()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Une passe de récolte a échoué : on conserve l'état courant et on réessaie au tick suivant.
            }
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
     * Pose un extracteur sur la tuile [cell] (index H3 de la tuile courante). Le décrément du stock et
     * le bâtiment posé **remontent seuls** par l'observation du document et de la sous-collection. Un
     * échec (réseau, refus défensif si la tuile vient d'être bâtie) ne fait pas régresser l'affichage.
     */
    fun placeExtracteur(cell: String) {
        viewModelScope.launch {
            try {
                placeExtractor(cell)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La pose a échoué : on conserve le dernier état connu, sans régression.
            }
        }
    }

    /**
     * Reflète la sous-collection des bâtiments dans [placedBuildings]. Un incident du flux conserve la
     * dernière liste connue, sans régression de l'affichage.
     */
    private suspend fun observeBuildings(id: PlayerId) {
        try {
            buildings.observe(id).collect { placed ->
                _placedBuildings.value = placed
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
