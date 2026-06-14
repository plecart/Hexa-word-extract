package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.core.geo.LatLng
import com.hexa.location.PositionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * État de la grille hexagonale affichée autour du joueur.
 *
 * Orchestre les contrats purs : il convertit la position filtrée ([PositionSource]) en cellule
 * courante via la façade [HexGrid], en déduit le disque de cellules visibles au zoom courant
 * ([VisibleCells]) et expose leurs contours à dessiner. La logique de sélection et de rendu vivant
 * ailleurs (pures, testées isolément), ce ViewModel reste une glu mince et testable avec une fausse
 * grille et des sources factices.
 *
 * Pour rester fluide, la grille n'est **recalculée que lorsque c'est nécessaire** : quand le joueur
 * change de cellule, ou quand le zoom franchit un palier d'anneaux — pas à chaque point GPS ni à
 * chaque micro-variation de zoom.
 *
 * @param positionSource source de la position filtrée, partagée avec la caméra (cf. [com.hexa.location.SharedPositionSource]).
 * @param grid façade de la grille hexagonale (cellule sous la position, disque, contours).
 */
class HexGridViewModel(
    positionSource: PositionSource,
    private val grid: HexGrid,
) : ViewModel() {
    /** Zoom courant de la carte ; mis à jour par l'UI au pincement. */
    private val zoom = MutableStateFlow(MapConfig.FOLLOW_ZOOM)

    /**
     * Contours des cellules de la grille à dessiner — une liste de sommets par cellule. Vide tant
     * qu'aucune position n'est connue. N'émet que tant qu'il est observé, pour libérer la position
     * partagée hors écran.
     */
    val outlines: StateFlow<List<List<LatLng>>> =
        combine(
            positionSource.positions().map(grid::cellAt).distinctUntilChanged(),
            zoom.distinctUntilChangedBy(VisibleCells::ringsForZoom),
        ) { center, zoomLevel ->
            VisibleCells.cellsAround(center, zoomLevel, grid).map(grid::outline)
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
