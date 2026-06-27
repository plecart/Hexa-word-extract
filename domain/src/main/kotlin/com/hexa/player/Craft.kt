package com.hexa.player

import com.hexa.config.Element
import com.hexa.config.GameConfig

/**
 * Verdict d'une tentative de craft : soit le bâtiment est construit (état joueur mis à jour), soit le
 * craft est refusé, motivé par les ressources manquantes.
 */
sealed interface CraftOutcome {
    /**
     * Craft réussi : [player] est le document mis à jour — inventaire débité de la recette et stock
     * du bâtiment construit incrémenté de un. À persister tel quel par l'appelant.
     */
    data class Built(val player: Player) : CraftOutcome

    /**
     * Craft refusé faute de ressources. [missing] donne, pour chaque élément en déficit, le nombre
     * d'unités manquantes (`requis - possédé`, toujours > 0) ; les éléments suffisants en sont absents.
     */
    data class Refused(val missing: Map<Element, Long>) : CraftOutcome
}

/**
 * Cœur **pur** du craft (cf. PRD #5, user stories 8-11) : applique une recette à un [Player] sans
 * aucune I/O — un document en entrée, un [CraftOutcome] en sortie. Toute la décision (suffisance,
 * débit, crédit) est ici, testable isolément ; la persistance vit dans `CraftBuildingUseCase`.
 *
 * La recette est lue depuis [GameConfig] (point unique d'équilibrage). Le mapping `BuildingType →
 * recette` est porté ici plutôt que dans `GameConfig` pour ne pas faire dépendre la config du domaine
 * joueur.
 */
object Craft {
    /**
     * Tente de construire un [type] depuis l'état de [player].
     *
     * @param player document joueur courant (inventaire + stock de bâtiments).
     * @param type bâtiment à construire (détermine la recette).
     * @return [CraftOutcome.Built] avec le document débité+crédité si les ressources suffisent, sinon
     *   [CraftOutcome.Refused] avec le détail des manquants — dans ce cas, [player] n'est pas modifié.
     */
    fun build(player: Player, type: BuildingType): CraftOutcome {
        val recipe = recipeOf(type)
        val missing = recipe
            .mapValues { (element, cost) -> cost - player.inventory[element] }
            .filterValues { it > 0 }
        if (missing.isNotEmpty()) return CraftOutcome.Refused(missing)

        val built = player.copy(inventory = player.inventory.minus(recipe)).incrementStock(type)
        return CraftOutcome.Built(built)
    }

    /** Recette du [type], en unités par élément (cf. [GameConfig]). */
    private fun recipeOf(type: BuildingType): Map<Element, Int> = when (type) {
        BuildingType.EXTRACTEUR -> GameConfig.RECIPE_EXTRACTEUR
    }
}
