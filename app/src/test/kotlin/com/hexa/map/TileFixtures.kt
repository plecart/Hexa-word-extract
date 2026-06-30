package com.hexa.map

import com.hexa.config.Element
import com.hexa.world.ElementDeposit
import com.hexa.world.TileContent

/**
 * Construit le contenu d'une tuile à partir des éléments présents (aucun = tuile vide), pour les tests
 * de coloration de la grille. Seul l'élément importe ici (la couleur ne dépend que de lui) ; richesse
 * et vitesse prennent des valeurs neutres. Pendant `:app` de `WorldFixtures` (interne à `:domain`).
 */
internal fun tile(vararg elements: Element): TileContent =
    TileContent(elements.map { ElementDeposit(it, richness = 0.5, ratePerHour = 1) })
