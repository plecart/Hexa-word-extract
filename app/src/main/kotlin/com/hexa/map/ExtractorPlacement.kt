package com.hexa.map

import com.hexa.player.PlacedBuilding
import com.hexa.player.PlacementDecision
import com.hexa.player.PlacementRules

/**
 * Décision de pose d'un extracteur pour la tuile [cell], à partir de son rapport observable au joueur.
 *
 * Cœur de glu **pure** partagé par les deux consommateurs de la carte — le marqueur « + » de la tuile
 * courante ([extractorPlacementCell]) et la **feuille d'inspection** de n'importe quelle tuile
 * ([TileInspectionViewModel], cf. #110). Il centralise l'unique règle d'occupation (« une tuile est
 * occupée si un bâtiment y est déjà posé ») en la dérivant des bâtiments posés, puis délègue la
 * décision au cœur métier [PlacementRules]. La garde contre une cellule **arbitraire** (couture de
 * sécurité) vit, elle, dans [com.hexa.player.PlaceExtractorUseCase], qui la revérifie côté écriture
 * (cf. #137).
 *
 * @param isCurrent la tuile [cell] est-elle la **tuile courante du joueur** (on ne pose que « sous ses
 *   pieds ») ; le marqueur « + » passe toujours `true` (sa cellule *est* la tuile courante par
 *   construction), l'inspection passe le rapport réel de la tuile inspectée.
 * @param cell index H3 **textuel** de la tuile (cf. [HexGrid.toH3String]) — sert à la fois de clé
 *   d'occupation et d'ID du futur document `buildings/{h3Index}`.
 * @param placedBuildings bâtiments déjà posés (source d'occupation, cf. [PlacedBuilding.cell]).
 * @param stock nombre d'extracteurs construits prêts à poser.
 * @return la [PlacementDecision] : [PlacementDecision.Placeable] si les trois conditions sont réunies,
 *   sinon [PlacementDecision.Refused] motivé par la première condition non remplie.
 */
fun placementDecisionFor(
    isCurrent: Boolean,
    cell: String,
    placedBuildings: List<PlacedBuilding>,
    stock: Int,
): PlacementDecision {
    val occupied = placedBuildings.any { it.cell == cell }
    return PlacementRules.decide(isCurrentTile = isCurrent, isTileOccupied = occupied, stock = stock)
}

/**
 * Détermine la tuile sur laquelle proposer la pose d'un extracteur — la **tuile courante** du joueur,
 * si elle est libre et qu'il reste des extracteurs en stock — ou `null` si aucune pose n'est possible.
 *
 * Glu **pure** entre l'état observable de la carte et [placementDecisionFor] : cette glu ne considère
 * **que** la tuile courante (elle *construit* sa cellule candidate depuis [currentTile]), donc la
 * condition « tuile courante » est structurellement satisfaite — `isCurrent = true` ici n'est pas une
 * garde court-circuitée mais un fait. Le résultat pilote l'apparition du marqueur « + » sur la carte.
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
    val decision = placementDecisionFor(
        isCurrent = true,
        cell = cell,
        placedBuildings = placedBuildings,
        stock = extractorStock,
    )
    return if (decision == PlacementDecision.Placeable) cell else null
}
