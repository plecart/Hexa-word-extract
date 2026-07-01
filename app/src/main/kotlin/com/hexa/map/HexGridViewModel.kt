package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.world.TileContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * État de la grille hexagonale destinée à l'affichage autour du joueur (alimente la source de
 * [Style.showHexGrid]).
 *
 * Orchestre les contrats purs : à partir de la **tuile courante** partagée ([currentTile], déjà
 * lissée par hystérésis en amont, cf. [com.hexa.HexaApplication.sharedCurrentTile]), il déduit le
 * disque de cellules du **rayon fixe** de la grille ([VisibleCells], [MapConfig.GRID_RENDER_RINGS]) et
 * expose chacune avec sa **teinte de remplissage** déjà résolue ([tileFillColor], depuis son contenu
 * [contentOf]). La tuile courante ne sert qu'à **centrer** le disque : elle n'est pas teintée
 * différemment des autres. Le suivi de tuile, la sélection du plus rare et le mapping couleur vivant
 * ailleurs (purs, testés isolément), ce ViewModel reste une glu mince et testable avec une fausse
 * grille, un faux générateur de contenu et un flux de tuile courante factice.
 *
 * Le rendu couvrant un rayon fixe indépendant du zoom, la grille n'est **recalculée que lorsque la
 * tuile courante change** — pas à chaque point GPS ni au pincement. Le contenu d'une tuile ([contentOf])
 * est régénéré de façon déterministe (jamais stocké), donc recalculé à coût négligeable à chaque
 * recomposition du disque.
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
    /**
     * Cellules de la grille à dessiner — chacune avec son contour et sa teinte de remplissage. Vide
     * tant qu'aucune position n'est connue. N'émet que tant qu'il est observé, pour libérer la position
     * partagée hors écran.
     */
    val cells: StateFlow<List<GridCell>> =
        currentTile.filterNotNull().distinctUntilChanged().map { current ->
            VisibleCells.cellsAround(current, grid).map { cell ->
                GridCell(grid.outline(cell), tileFillColor(contentOf(cell)))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), emptyList())
}
