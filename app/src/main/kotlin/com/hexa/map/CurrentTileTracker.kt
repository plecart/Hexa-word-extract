package com.hexa.map

import com.hexa.core.geo.GreatCircle
import com.hexa.core.geo.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.scan

/**
 * Suivi **pur** de la tuile courante avec hystérésis.
 *
 * Sans hystérésis, la cellule courante bascule pile au franchissement de la frontière H3 : à l'arrêt
 * en bordure, le tremblement résiduel du GPS lissé fait clignoter la tuile entre deux cellules. Ce
 * module ne bascule que lorsque le joueur est *franchement* dans la nouvelle cellule — son centre
 * franchement plus proche que celui de la tuile courante, d'au moins une marge (cf.
 * [MapConfig.TILE_HYSTERESIS_MARGIN_M]).
 *
 * Pendant de [PositionFilter][com.hexa.location.PositionFilter] côté tuile : [next] est la décision
 * pure (un pas), [currentTile] l'applique en cascade sur un flux. Pas de dépendance Android, ni H3
 * (la géométrie passe par la façade [HexGrid]) : testable avec une fausse grille.
 */
object CurrentTileTracker {
    /**
     * Tuile courante après observation de [position], partant de la tuile [current].
     *
     * @param current tuile courante précédente, ou `null` à l'amorçage (première position).
     * @param position position lissée observée.
     * @param grid façade fournissant la cellule sous une position et le centre d'une cellule.
     * @param hysteresisMarginM marge d'hystérésis en mètres : écart de distance aux centres requis
     *   pour basculer. `0` revient à suivre la cellule géométrique sans hystérésis.
     * @return la tuile courante : [current] tant que la candidate n'est pas franchement plus proche,
     *   sinon la cellule sous [position] (et toujours cette dernière à l'amorçage).
     */
    fun next(current: Long?, position: LatLng, grid: HexGrid, hysteresisMarginM: Double): Long {
        val candidate = grid.cellAt(position)
        if (current == null || candidate == current) return current ?: candidate
        val toCandidate = GreatCircle.distanceMeters(position, grid.centerOf(candidate))
        val toCurrent = GreatCircle.distanceMeters(position, grid.centerOf(current))
        return if (toCandidate + hysteresisMarginM < toCurrent) candidate else current
    }

    /**
     * Transforme un flux de positions lissées en flux de la **tuile courante**, lissé par hystérésis.
     * N'émet qu'à chaque *changement* de tuile (première tuile comprise) ; les positions qui ne font
     * pas bouger la tuile courante n'émettent rien.
     */
    fun Flow<LatLng>.currentTile(grid: HexGrid, hysteresisMarginM: Double): Flow<Long> =
        scan<LatLng, Long?>(null) { current, position -> next(current, position, grid, hysteresisMarginM) }
            .filterNotNull()
            .distinctUntilChanged()
}
