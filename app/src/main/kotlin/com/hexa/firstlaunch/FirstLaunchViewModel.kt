package com.hexa.firstlaunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.location.PositionSource
import com.hexa.map.CurrentTileTracker.currentTile
import com.hexa.map.HexGrid
import com.hexa.map.MapConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État de l'écran de **premier lancement** (cf. PRD #5, user stories 3-5). Tant que le joueur n'a pas
 * de base, cet écran l'invite à se rendre à l'endroit voulu et à poser sa base.
 *
 * Glu mince : il suit la **tuile courante** ([currentTile], via [CurrentTileTracker], comme la grille)
 * pour n'activer le bouton « Poser ma base ici » que lorsque la position est connue, et délègue la
 * pose à [placeBaseAt] (le cas d'usage de pose, cf. [com.hexa.player.PlaceBaseUseCase]) en convertissant
 * d'abord la tuile en son index H3 textuel via [grid]. La logique métier (écriture de `baseCell`, doc
 * bâtiment) vit dans le cas d'usage : ce ViewModel reste testable avec une fausse grille et une source
 * de position factice.
 *
 * @param positionSource position GPS filtrée partagée (cf. [com.hexa.HexaApplication.sharedPositionSource]).
 * @param grid façade de la grille (cellule sous la position, centre, index H3 textuel).
 * @param placeBaseAt pose la base sur l'index H3 fourni (frontière vers le cas d'usage).
 * @param hysteresisMarginM marge d'hystérésis du suivi de tuile courante, en mètres.
 */
class FirstLaunchViewModel(
    positionSource: PositionSource,
    private val grid: HexGrid,
    private val placeBaseAt: suspend (cell: String) -> Unit,
    hysteresisMarginM: Double = MapConfig.TILE_HYSTERESIS_MARGIN_M,
) : ViewModel() {
    /**
     * Tuile courante sous le joueur, ou `null` tant qu'aucune position n'est connue (GPS non fixé) :
     * le bouton de pose n'est actif que lorsqu'elle ne l'est pas.
     */
    val currentTile: StateFlow<Long?> =
        positionSource
            .positions()
            .currentTile(grid, hysteresisMarginM)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), null)

    private val _placing = MutableStateFlow(false)

    /** `true` pendant l'écriture de la pose : empêche un double envoi (cf. garde dans [placeBase]). */
    val placing: StateFlow<Boolean> = _placing.asStateFlow()

    private val _placementFailed = MutableStateFlow(false)

    /**
     * `true` quand la **dernière** tentative de pose a échoué (réseau, Firestore), pour inviter le
     * joueur à réessayer. Contrairement aux flux récurrents de [com.hexa.player.PlayerViewModel]
     * (récolte/craft) où un échec est avalé car une prochaine passe le rattrape, la pose de base est
     * un **acte unique sans réessai automatique** : un échec silencieux laisserait le joueur croire
     * que la pose a réussi. On le rend donc visible. Remis à `false` au début de chaque tentative.
     */
    val placementFailed: StateFlow<Boolean> = _placementFailed.asStateFlow()

    /**
     * Pose la base sur la tuile courante. Sans tuile connue ou pendant qu'une pose est déjà en cours,
     * n'a aucun effet (anti-double-envoi). En cas d'échec de persistance, lève [placementFailed].
     */
    fun placeBase() {
        val cell = currentTile.value ?: return
        if (_placing.value) return
        _placing.value = true
        _placementFailed.value = false
        viewModelScope.launch {
            try {
                placeBaseAt(grid.toH3String(cell))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _placementFailed.value = true
            } finally {
                _placing.value = false
            }
        }
    }
}
