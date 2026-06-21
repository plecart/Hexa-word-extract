package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.location.PositionSource
import com.hexa.map.CurrentTileTracker.currentTile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * État de la grille hexagonale affichée autour du joueur.
 *
 * Orchestre les contrats purs : il convertit la position filtrée ([PositionSource]) en **tuile
 * courante** via [CurrentTileTracker] (avec hystérésis, pour ne pas clignoter en bordure), en déduit
 * le disque de cellules visibles au zoom courant ([VisibleCells]) et expose chacune avec son état
 * visuel ([tileState] : courante, bâtie, normale). La logique de suivi, de sélection et de
 * classification vivant ailleurs (pures, testées isolément), ce ViewModel reste une glu mince et
 * testable avec une fausse grille et des sources factices.
 *
 * Pour rester fluide, la grille n'est **recalculée que lorsque c'est nécessaire** : quand la tuile
 * courante change, ou quand le zoom franchit un palier d'anneaux — pas à chaque point GPS ni à
 * chaque micro-variation de zoom.
 *
 * @param positionSource source de la position filtrée, partagée avec la caméra (cf. [com.hexa.location.SharedPositionSource]).
 * @param grid façade de la grille hexagonale (cellule sous la position, disque, contours, centre).
 * @param builtCells flux des **index H3 textuels** des cellules bâties, pour l'état [TileState.BATIE].
 *   Au MVP, la seule cellule bâtie est la base posée (`Player.baseCell`) ; ce flux émet à nouveau dès
 *   qu'elle change, ce qui fait apparaître la base sur la grille sans recréer le ViewModel.
 * @param hysteresisMarginM marge d'hystérésis du suivi de tuile courante, en mètres.
 */
class HexGridViewModel(
    positionSource: PositionSource,
    private val grid: HexGrid,
    builtCells: Flow<Set<String>> = flowOf(emptySet()),
    private val hysteresisMarginM: Double = MapConfig.TILE_HYSTERESIS_MARGIN_M,
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
            positionSource.positions().currentTile(grid, hysteresisMarginM),
            zoom.distinctUntilChangedBy(VisibleCells::ringsForZoom),
            builtCells,
        ) { current, zoomLevel, built ->
            val builtTiles = BuiltTiles { grid.toH3String(it) in built }
            VisibleCells.cellsAround(current, zoomLevel, grid).map { cell ->
                GridCell(grid.outline(cell), tileState(cell, current, builtTiles))
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
