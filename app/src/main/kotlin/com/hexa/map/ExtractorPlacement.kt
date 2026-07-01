package com.hexa.map

import com.hexa.player.PlacedBuilding
import com.hexa.player.PlacementDecision
import com.hexa.player.PlacementRules

/**
 * Détermine la tuile sur laquelle proposer la pose d'un extracteur — la **tuile courante** du joueur,
 * si elle est libre et qu'il reste des extracteurs en stock — ou `null` si aucune pose n'est possible.
 *
 * Glu **pure** entre l'état observable de la carte (tuile courante, bâtiments posés, stock) et le cœur
 * de décision [PlacementRules] : cette glu ne considère **que** la tuile courante (elle *construit* sa
 * cellule candidate depuis [currentTile]), donc la condition « tuile courante » est structurellement
 * satisfaite — `isCurrentTile = true` ici n'est pas une garde court-circuitée mais un fait ; restent
 * l'occupation et le stock. La garde contre une cellule **arbitraire** (couture de sécurité) vit, elle,
 * dans [com.hexa.player.PlaceExtractorUseCase], qui compare la cellule visée à la tuile courante réelle
 * (cf. #137). Le résultat pilote l'apparition du marqueur « + » sur la carte.
 *
 * @param currentTile tuile courante du joueur (cf. [com.hexa.HexaApplication.sharedCurrentTile]) ;
 *   `null` tant qu'aucune position n'est connue.
 * @param placedBuildings bâtiments déjà posés (source d'occupation, cf. [PlacedBuilding.cell]).
 * @param extractorStock nombre d'extracteurs construits prêts à poser.
 * @param toH3String résolution de la cellule H3 numérique vers son index textuel (cf.
 *   [HexGrid.toH3String]) — l'ID du futur document `buildings/{h3Index}`.
 * @return l'index H3 textuel de la tuile où afficher le marqueur « + », ou `null` si la pose n'est pas
 *   possible.
 */
fun extractorPlacementCell(
    currentTile: Long?,
    placedBuildings: List<PlacedBuilding>,
    extractorStock: Int,
    toH3String: (Long) -> String,
): String? {
    val cell = toH3String(currentTile ?: return null)
    val occupied = placedBuildings.any { it.cell == cell }
    val decision = PlacementRules.decide(isCurrentTile = true, isTileOccupied = occupied, stock = extractorStock)
    return if (decision == PlacementDecision.Placeable) cell else null
}
