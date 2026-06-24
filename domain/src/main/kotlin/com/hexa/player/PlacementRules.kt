package com.hexa.player

/**
 * Raison du refus d'une pose, quand [PlacementRules] n'autorise pas le placement. Chaque cas
 * correspond à une condition non remplie, exposée distinctement pour motiver l'UI (l'affordance « + »
 * n'apparaît que sur un [PlacementDecision.Placeable]).
 */
enum class PlacementRefusal {
    /** La tuile inspectée n'est pas la tuile courante du joueur (on ne pose que « sous ses pieds »). */
    NOT_CURRENT_TILE,

    /** La tuile porte déjà un bâtiment (placement permanent, un seul bâtiment par tuile). */
    TILE_OCCUPIED,

    /** Aucun bâtiment construit en stock à poser. */
    NO_STOCK,
}

/**
 * Décision de placement : la pose est possible, ou refusée avec sa raison.
 */
sealed interface PlacementDecision {
    /** La pose est autorisée : les trois conditions sont réunies. */
    data object Placeable : PlacementDecision

    /** La pose est refusée, motivée par [reason]. */
    data class Refused(val reason: PlacementRefusal) : PlacementDecision
}

/**
 * Cœur **pur** des règles de placement d'un bâtiment (cf. PRD #4, user stories 5-7) : aucune I/O,
 * trois données en entrée, une [PlacementDecision] en sortie. Toute la décision (les trois conditions
 * et chaque raison de refus) vit ici, testable isolément ; la persistance vit dans
 * [PlaceExtractorUseCase], qui réutilise cette décision avant d'écrire.
 */
object PlacementRules {
    /**
     * Décide si un bâtiment peut être posé sur la tuile inspectée.
     *
     * Les conditions sont évaluées dans un ordre **stable** — tuile courante, puis occupation, puis
     * stock — si bien qu'en cas d'échecs multiples, la première condition non remplie motive le refus.
     *
     * @param isCurrentTile la tuile inspectée est-elle la tuile courante du joueur.
     * @param isTileOccupied la tuile porte-t-elle déjà un bâtiment.
     * @param stock nombre de bâtiments construits disponibles à poser.
     * @return [PlacementDecision.Placeable] si les trois conditions sont réunies, sinon
     *   [PlacementDecision.Refused] avec la première raison rencontrée.
     */
    fun decide(isCurrentTile: Boolean, isTileOccupied: Boolean, stock: Int): PlacementDecision {
        if (!isCurrentTile) return PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)
        if (isTileOccupied) return PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)
        if (stock <= 0) return PlacementDecision.Refused(PlacementRefusal.NO_STOCK)
        return PlacementDecision.Placeable
    }
}
