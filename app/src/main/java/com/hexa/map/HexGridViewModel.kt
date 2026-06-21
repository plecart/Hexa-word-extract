package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

/**
 * État de la grille hexagonale affichée autour du joueur.
 *
 * Orchestre les contrats purs : à partir de la **tuile courante** partagée ([currentTile], déjà
 * lissée par hystérésis en amont, cf. [com.hexa.HexaApplication.sharedCurrentTile]), il déduit le
 * disque de cellules visibles au zoom courant ([VisibleCells]) et expose chacune avec son état
 * visuel ([tileState] : courante ou normale). Le suivi de tuile, la sélection et la classification
 * vivant ailleurs (purs, testés isolément), ce ViewModel reste une glu mince et testable avec une
 * fausse grille et un flux de tuile courante factice.
 *
 * Pour rester fluide, la grille n'est **recalculée que lorsque c'est nécessaire** : quand la tuile
 * courante change, ou quand le zoom franchit un palier d'anneaux — pas à chaque point GPS ni à
 * chaque micro-variation de zoom.
 *
 * @param currentTile flux de la **tuile courante partagée** (`null` tant qu'aucune position n'est
 *   connue) ; la même source alimente l'inspection de tuile, pour une seule intégration H3 vivante.
 * @param grid façade de la grille hexagonale (disque, contours, index textuel).
 */
class HexGridViewModel(
    currentTile: Flow<Long?>,
    private val grid: HexGrid,
) : ViewModel() {
    /** Zoom courant de la carte ; mis à jour par l'UI au pincement. */
    private val zoom = MutableStateFlow(MapConfig.FOLLOW_ZOOM)

    /**
     * Cellules de la grille à dessiner — chacune avec son contour et son [TileState]. Vide tant
     * qu'aucune position n'est connue. N'émet que tant qu'il est observé, pour libérer la position
     * partagée hors écran.
     */
    val cells: StateFlow<List<GridCell>> =
        combine(
            currentTile.filterNotNull(),
            zoom.distinctUntilChangedBy(VisibleCells::ringsForZoom),
        ) { current, zoomLevel ->
            VisibleCells.cellsAround(current, zoomLevel, grid).map { cell ->
                GridCell(grid.outline(cell), tileState(cell, current))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** À appeler quand l'utilisateur ajuste le zoom : la grille suivra le palier d'anneaux. */
    fun onZoomChanged(zoomLevel: Double) {
        zoom.value = zoomLevel
    }

    private companion object {
        /** Délai de maintien de la position partagée après la dernière désinscription (rotation d'écran). */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
