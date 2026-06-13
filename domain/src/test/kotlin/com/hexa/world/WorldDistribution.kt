package com.hexa.world

import com.hexa.config.Element
import com.hexa.config.GameConfig
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Agrégats mesurés de la distribution du monde sur un échantillon de tuiles.
 *
 * Ne contient que des comptages bruts ; les taux (présence, vide, moyenne) en sont **dérivés** à la
 * demande. Tous les éléments de [Element] figurent dans [presenceCount] et [richnessByElement], même
 * absents (valeur 0 / liste vide).
 *
 * @property totalTiles nombre de tuiles échantillonnées.
 * @property presenceCount nombre de tuiles où chaque élément est présent.
 * @property emptyTiles nombre de tuiles sans aucun élément.
 * @property richnessByElement richesses observées pour chaque élément là où il est présent.
 */
data class DistributionStats(
    val totalTiles: Int,
    val presenceCount: Map<Element, Int>,
    val emptyTiles: Int,
    val richnessByElement: Map<Element, List<Double>>,
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
        val richnessByElement = Element.entries.associateWith { mutableListOf<Double>() }
        var emptyTiles = 0
        for (content in contents) {
            if (content.deposits.isEmpty()) emptyTiles++
            for (deposit in content.deposits) {
                presenceCount[deposit.element] = presenceCount.getValue(deposit.element) + 1
                richnessByElement.getValue(deposit.element).add(deposit.richness)
            }
        }
        return DistributionStats(
            totalTiles = contents.size,
            presenceCount = presenceCount,
            emptyTiles = emptyTiles,
            richnessByElement = richnessByElement,
        )
    }

    /**
     * Propose, pour chaque élément cible, le seuil de présence qui atteindrait sa fréquence visée —
     * **sans jamais lire les valeurs internes du bruit**.
     *
     * Méthode : pour les tuiles où [referenceElement] est présent, on reconstruit sa valeur de bruit
     * normalisée `v = seuil + richesse × (1 − seuil)` à partir de la richesse (sortie publique). Les
     * cinq éléments partageant la **même** transformation bruit→[0,1] (distribution marginale
     * identique), les quantiles empiriques de `v` issus de la référence donnent les seuils visant
     * chaque cible. La référence ([Element.CENDRITE], seuil le plus bas) couvre tous les quantiles
     * utiles ; pour une cible plus fréquente que la référence — hors de cette couverture — le rang
     * est borné, donnant un seuil proche de celui de la référence.
     *
     * Ne modifie aucune constante : la proposition est destinée au mainteneur (pas de recalage
     * silencieux, cf. critères de #16).
     *
     * @param stats mesure dont on lit les richesses de [referenceElement] et son taux de présence.
     * @param presenceTargets fréquence de présence visée (dans `]0, 1]`) par élément.
     * @param referenceElement élément servant de référence à la reconstruction (le plus commun).
     * @return le seuil de présence proposé par élément cible.
     */
    fun proposeThresholds(
        stats: DistributionStats,
        presenceTargets: Map<Element, Double>,
        referenceElement: Element = Element.CENDRITE,
    ): Map<Element, Double> {
        val referenceThreshold = GameConfig.PRESENCE_THRESHOLDS[referenceElement.ordinal]
        val referenceValues = stats.richnessByElement.getValue(referenceElement)
            .map { richness -> referenceThreshold + richness * (1.0 - referenceThreshold) }
            .sorted()
        val referencePresence = stats.presenceRate(referenceElement)
        return presenceTargets.mapValues { (_, target) ->
            // Seuil = quantile plein (1 − cible) de v, repositionné dans le segment des présents.
            val fullQuantile = 1.0 - target
            val rankAmongPresent = ((fullQuantile - (1.0 - referencePresence)) / referencePresence)
                .coerceIn(0.0, 1.0)
            quantile(referenceValues, rankAmongPresent)
        }
    }

    /** Quantile au rang fractionnaire [rank] (`[0, 1]`) d'une liste **triée**, par interpolation linéaire. */
    private fun quantile(sorted: List<Double>, rank: Double): Double {
        if (sorted.size == 1) return sorted.single()
        val position = rank * (sorted.size - 1)
        val low = floor(position).toInt()
        val high = ceil(position).toInt()
        return sorted[low] + (sorted[high] - sorted[low]) * (position - low)
    }
}
