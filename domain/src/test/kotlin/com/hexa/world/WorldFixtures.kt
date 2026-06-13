package com.hexa.world

import com.hexa.config.Element

/** Construit un gisement d'élément pour les tests de distribution (valeurs par défaut neutres). */
internal fun deposit(element: Element, richness: Double = 0.5, rate: Int = 1) = ElementDeposit(element, richness, rate)

/** Construit le contenu d'une tuile à partir de gisements (aucun argument = tuile vide). */
internal fun tile(vararg deposits: ElementDeposit) = TileContent(deposits.toList())
