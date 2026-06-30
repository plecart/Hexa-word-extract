package com.hexa.world

import com.hexa.config.Element

/**
 * Gisement d'un élément présent sur une tuile.
 *
 * Produit par le générateur de monde, consommé par l'inspection de tuile (#19) et la récolte (#25).
 * Une instance n'existe que pour un élément **effectivement présent** : la richesse y est donc
 * strictement positive.
 *
 * @property element l'élément collectable.
 * @property richness richesse du gisement, dans `]0, 1]` : 1 = gisement maximal.
 * @property ratePerHour vitesse d'extraction en unités/heure, **arrondie à l'entier** ; comprise
 *   entre le plancher ([com.hexa.config.GameConfig.RATE_FLOOR] du taux de base) et le taux de base
 *   de l'élément.
 */
data class ElementDeposit(
    val element: Element,
    val richness: Double,
    val ratePerHour: Int,
)

/**
 * Contenu complet d'une tuile : la liste des éléments qui y sont présents, chacun avec son gisement.
 *
 * L'ordre suit la rareté croissante de [Element] (seuls les éléments présents figurent). Une tuile
 * sans aucun élément a une liste [deposits] vide — cas attendu pour environ un quart des tuiles.
 *
 * @property deposits gisements présents sur la tuile ; vide si la tuile ne contient rien.
 */
data class TileContent(val deposits: List<ElementDeposit>)

/**
 * L'élément **le plus rare** présent sur la tuile, ou `null` si elle ne contient aucun gisement.
 *
 * La rareté suit l'ordre déclaré de [Element] (croissante, cf. [Element]) : le plus rare est celui
 * de plus grand `ordinal` parmi les gisements présents. La sélection se fait sur l'`ordinal`
 * **indépendamment de l'ordre de [deposits]** — robuste si cet ordre venait à changer. Pure,
 * déterministe, sans dépendance de rendu : elle alimente la recoloration des hexagones (#126), où la
 * teinte d'une tuile vient de l'identité de son élément le plus rare.
 */
val TileContent.rarestElement: Element?
    get() = deposits.maxByOrNull { it.element.ordinal }?.element
