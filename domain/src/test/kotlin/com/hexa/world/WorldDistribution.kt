package com.hexa.world

import com.hexa.config.Element

/**
 * Agrégats mesurés de la distribution du monde sur un échantillon de tuiles.
 *
 * Ne contient que des comptages bruts ; les taux (présence, vide, moyenne) en sont **dérivés** à la
 * demande. Tous les éléments de [Element] figurent dans [presenceCount], même absents (valeur 0).
 *
 * @property totalTiles nombre de tuiles échantillonnées.
 * @property presenceCount nombre de tuiles où chaque élément est présent.
 * @property emptyTiles nombre de tuiles sans aucun élément.
 */
data class DistributionStats(
    val totalTiles: Int,
    val presenceCount: Map<Element, Int>,
    val emptyTiles: Int,
) {
    /** Fraction des tuiles où [element] est présent, dans `[0, 1]`. */
    fun presenceRate(element: Element): Double = presenceCount.getValue(element).toDouble() / totalTiles

    /** Fraction des tuiles sans aucun élément, dans `[0, 1]`. */
    val emptyRate: Double get() = emptyTiles.toDouble() / totalTiles

    /** Nombre moyen d'éléments présents par tuile. */
    val averageElementsPerTile: Double get() = presenceCount.values.sum().toDouble() / totalTiles
}

/**
 * Mesure pure de la distribution du monde : agrège une liste de [TileContent] en [DistributionStats].
 *
 * N'accède qu'à la **sortie publique** du générateur (les éléments présents et leur richesse),
 * jamais aux valeurs internes du bruit — conformément aux décisions de test du PRD #3.
 */
object WorldDistribution {
    /**
     * Agrège les contenus de tuiles échantillonnés.
     *
     * @param contents contenus produits par le générateur sur l'échantillon de cellules.
     * @return les comptages de présence, de tuiles vides et le total.
     */
    fun measure(contents: List<TileContent>): DistributionStats {
        val presenceCount = Element.entries.associateWith { 0 }.toMutableMap()
        var emptyTiles = 0
        for (content in contents) {
            if (content.deposits.isEmpty()) emptyTiles++
            for (deposit in content.deposits) {
                presenceCount[deposit.element] = presenceCount.getValue(deposit.element) + 1
            }
        }
        return DistributionStats(totalTiles = contents.size, presenceCount = presenceCount, emptyTiles = emptyTiles)
    }
}
