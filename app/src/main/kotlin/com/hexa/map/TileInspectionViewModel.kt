package com.hexa.map

import androidx.lifecycle.ViewModel
import com.hexa.core.geo.LatLng
import com.hexa.player.PlacedBuilding
import com.hexa.world.TileContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État du panneau d'inspection de tuile, piloté par les taps sur la carte.
 *
 * Glu mince entre des contrats purs : la **grille** ([grid]) résout la cellule H3 sous le point tapé
 * (et l'exprime en index textuel via [HexGrid.toH3String]), le **générateur de monde** ([contentOf])
 * en recalcule le contenu *à la volée* (jamais stocké — d'où l'ouverture instantanée et des valeurs
 * identiques d'une consultation à l'autre, le générateur étant déterministe), la **tuile courante
 * partagée** ([currentTile]) dit si la tuile inspectée est celle du joueur, et [placedBuildings] +
 * [extractorStock] alimentent la **décision de pose** déléguée à [placementDecisionFor]. Aucune
 * dépendance Android ni H3 directe : testable avec une fausse grille et de faux flux.
 *
 * @param grid façade de la grille hexagonale ; [HexGrid.cellAt] (point tapé → cellule) et
 *   [HexGrid.toH3String] (cellule → clé d'occupation) sont utilisées ici.
 * @param contentOf source du contenu d'une tuile à partir de son index H3 — l'interface publique du
 *   générateur de monde ([com.hexa.world.WorldGenerator.contentOf]).
 * @param currentTile tuile courante du joueur (partagée, cf. [com.hexa.HexaApplication.sharedCurrentTile]) ;
 *   `null` tant qu'aucune position n'est connue, auquel cas aucune tuile n'est « courante ».
 * @param placedBuildings bâtiments déjà posés (source d'occupation de la décision de pose), lus à la
 *   dernière valeur au moment de l'inspection.
 * @param extractorStock nombre d'extracteurs construits prêts à poser, lu à la dernière valeur au
 *   moment de l'inspection.
 */
class TileInspectionViewModel(
    private val grid: HexGrid,
    private val contentOf: (Long) -> TileContent,
    private val currentTile: StateFlow<Long?>,
    private val placedBuildings: StateFlow<List<PlacedBuilding>>,
    private val extractorStock: StateFlow<Int>,
) : ViewModel() {
    private val _inspection = MutableStateFlow<TileInspection?>(null)

    /** Panneau d'inspection courant, ou `null` quand il est fermé. */
    val inspection: StateFlow<TileInspection?> = _inspection.asStateFlow()

    /**
     * Inspecte la tuile sous le point carte [position] : résout sa cellule, en recalcule le contenu,
     * dérive la possibilité de pose et ouvre le panneau. Remplace une éventuelle inspection précédente.
     * Le stock et les bâtiments posés sont lus à leur **dernière** valeur (`.value`), comme la tuile
     * courante — l'inspection est un instantané au moment du tap.
     */
    fun inspectAt(position: LatLng) {
        val cell = grid.cellAt(position)
        val isCurrent = cell == currentTile.value
        _inspection.value = TileInspection(
            deposits = contentOf(cell).deposits,
            isCurrent = isCurrent,
            placement = placementDecisionFor(
                isCurrent = isCurrent,
                cell = grid.toH3String(cell),
                placedBuildings = placedBuildings.value,
                stock = extractorStock.value,
            ),
        )
    }

    /** Ferme le panneau d'inspection. */
    fun dismiss() {
        _inspection.value = null
    }
}
