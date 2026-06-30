package com.hexa.map

import androidx.lifecycle.ViewModel
import com.hexa.core.geo.LatLng
import com.hexa.world.TileContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État du panneau d'inspection de tuile, piloté par les taps sur la carte.
 *
 * Glu mince entre trois contrats purs : la **grille** ([grid]) résout la cellule H3 sous le point
 * tapé, le **générateur de monde** ([contentOf]) en recalcule le contenu *à la volée* (jamais stocké
 * — d'où l'ouverture instantanée et des valeurs identiques d'une consultation à l'autre, le générateur
 * étant déterministe), et la **tuile courante partagée** ([currentTile]) dit si la tuile inspectée est
 * celle du joueur. Aucune dépendance Android ni H3 directe : testable avec une fausse grille et un faux
 * générateur.
 *
 * @param grid façade de la grille hexagonale ; seule [HexGrid.cellAt] est utilisée ici (point tapé →
 *   cellule).
 * @param contentOf source du contenu d'une tuile à partir de son index H3 — l'interface publique du
 *   générateur de monde ([com.hexa.world.WorldGenerator.contentOf]).
 * @param currentTile tuile courante du joueur (partagée, cf. [com.hexa.HexaApplication.sharedCurrentTile]) ;
 *   `null` tant qu'aucune position n'est connue, auquel cas aucune tuile n'est « courante ».
 */
class TileInspectionViewModel(
    private val grid: HexGrid,
    private val contentOf: (Long) -> TileContent,
    private val currentTile: StateFlow<Long?>,
) : ViewModel() {
    private val _inspection = MutableStateFlow<TileInspection?>(null)

    /** Panneau d'inspection courant, ou `null` quand il est fermé. */
    val inspection: StateFlow<TileInspection?> = _inspection.asStateFlow()

    /**
     * Inspecte la tuile sous le point carte [position] : résout sa cellule, en recalcule le contenu et
     * ouvre le panneau. Remplace une éventuelle inspection précédente.
     */
    fun inspectAt(position: LatLng) {
        val cell = grid.cellAt(position)
        _inspection.value = TileInspection(
            deposits = contentOf(cell).deposits,
            isCurrent = cell == currentTile.value,
        )
    }

    /** Ferme le panneau d'inspection. */
    fun dismiss() {
        _inspection.value = null
    }
}
