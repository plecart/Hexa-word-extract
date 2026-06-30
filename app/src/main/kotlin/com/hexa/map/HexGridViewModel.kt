package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.world.TileContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

/**
 * État de la grille hexagonale destinée à l'affichage autour du joueur (alimente la source de
 * [Style.showHexGrid]).
 *
 * Orchestre les contrats purs : à partir de la **tuile courante** partagée ([currentTile], déjà
 * lissée par hystérésis en amont, cf. [com.hexa.HexaApplication.sharedCurrentTile]), il déduit le
 * disque de cellules visibles au zoom courant ([VisibleCells]) et expose chacune avec sa **teinte de
 * remplissage** déjà résolue ([tileFillColor], depuis son contenu [contentOf]). La tuile courante ne
 * sert qu'à **centrer** le disque : elle n'est pas teintée différemment des autres. Le suivi de tuile,
 * la sélection du plus rare et le mapping couleur vivant ailleurs (purs, testés isolément), ce ViewModel
 * reste une glu mince et testable avec une fausse grille, un faux générateur de contenu et un flux de
 * tuile courante factice.
 *
 * Pour rester fluide, la grille n'est **recalculée que lorsque c'est nécessaire** : quand la tuile
 * courante change, ou quand le zoom franchit un palier d'anneaux — pas à chaque point GPS ni à
 * chaque micro-variation de zoom. Le contenu d'une tuile ([contentOf]) est régénéré de façon
 * déterministe (jamais stocké), donc recalculé à coût négligeable à chaque recomposition du disque.
 *
 * @param currentTile flux de la **tuile courante partagée** (`null` tant qu'aucune position n'est
 *   connue) ; la même source alimente l'inspection de tuile, pour une seule intégration H3 vivante.
 * @param grid façade de la grille hexagonale (disque, contours, index textuel).
 * @param contentOf source du contenu d'une tuile depuis son index H3 (gisements), pour en teinter la
 *   cellule ; l'interface publique du générateur de monde ([com.hexa.world.WorldGenerator.contentOf]),
 *   la **même** que celle de l'inspection de tuile.
 */
class HexGridViewModel(
    currentTile: Flow<Long?>,
    private val grid: HexGrid,
    private val contentOf: (Long) -> TileContent,
) : ViewModel() {
    /** Zoom courant de la carte ; mis à jour par l'UI au pincement. */
    private val zoom = MutableStateFlow(MapConfig.FOLLOW_ZOOM)

    /**
     * Cellules de la grille à dessiner — chacune avec son contour et sa teinte de remplissage. Vide
     * tant qu'aucune position n'est connue. N'émet que tant qu'il est observé, pour libérer la position
     * partagée hors écran.
     */
    val cells: StateFlow<List<GridCell>> =
        combine(
            currentTile.filterNotNull(),
            zoom.distinctUntilChangedBy(VisibleCells::ringsForZoom),
        ) { current, zoomLevel ->
            VisibleCells.cellsAround(current, zoomLevel, grid).map { cell ->
                GridCell(grid.outline(cell), tileFillColor(contentOf(cell)))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), emptyList())

    /** À appeler quand l'utilisateur ajuste le zoom : la grille suivra le palier d'anneaux. */
    fun onZoomChanged(zoomLevel: Double) {
        zoom.value = zoomLevel
    }
}
