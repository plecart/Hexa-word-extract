package com.hexa.map

import com.hexa.player.PlacementDecision
import com.hexa.world.ElementDeposit

/**
 * État du panneau d'inspection d'une tuile : son contenu et son rapport au joueur.
 *
 * Produit à la volée par [TileInspectionViewModel] au tap, jamais stocké : le contenu est recalculé
 * par le générateur de monde à chaque ouverture. Une instance non nulle signifie « panneau ouvert » ;
 * `null` côté ViewModel signifie « panneau fermé ».
 *
 * @property deposits gisements présents sur la tuile, par rareté croissante ; vide si la tuile ne
 *   contient rien ([isEmpty]) — l'état vide explicite demandé par l'inspection.
 * @property isCurrent `true` si la tuile inspectée est la **tuile courante du joueur** (badge « vous
 *   êtes ici »).
 * @property placement possibilité de poser un extracteur sur la tuile inspectée, ou sa raison de refus
 *   (cf. [placementDecisionFor]) — affichée en clair par l'inspection, indépendamment du contenu de la
 *   tuile ; « pose possible » n'apparaît que sur une tuile courante, libre et approvisionnée.
 */
data class TileInspection(
    val deposits: List<ElementDeposit>,
    val isCurrent: Boolean,
    val placement: PlacementDecision,
) {
    /** La tuile ne contient aucun gisement : le panneau affiche son état vide. */
    val isEmpty: Boolean get() = deposits.isEmpty()
}
