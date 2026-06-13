package com.hexa.world

import com.hexa.config.Element
import com.hexa.config.GameConfig
import java.util.Locale

/**
 * Cibles de distribution visées par le game design (§3.4), **provisoires**. Servent de référence de
 * comparaison au rapport : valeurs vers lesquelles le recalage des seuils doit tendre.
 */
object DistributionTargets {
    /** Fréquence de présence visée par élément (ordre de rareté croissante). */
    val presenceByElement: Map<Element, Double> = mapOf(
        Element.CENDRITE to 0.55,
        Element.GIVRELIN to 0.30,
        Element.LITHOSEVE to 0.15,
        Element.ECHOFER to 0.07,
        Element.NYCTITE to 0.03,
    )

    /** Nombre moyen d'éléments par tuile visé. */
    const val AVERAGE_ELEMENTS_PER_TILE: Double = 1.1

    /** Proportion de tuiles vides visée. */
    const val EMPTY_RATE: Double = 0.25
}

/**
 * Rend une [DistributionStats] en **rapport texte lisible** : cible vs mesuré par élément, moyennes
 * globales, et seuils proposés pour atteindre les cibles (constantes [GameConfig] inchangées).
 *
 * Tous les nombres sont formatés en [Locale.ROOT] (point décimal) pour un rapport **identique sur
 * toute machine** — dev (locale FR) comme CI (locale US).
 */
object WorldDistributionReport {
    /**
     * @param stats agrégats mesurés sur l'échantillon.
     * @return le rapport multi-lignes, prêt à imprimer ou à écrire dans un fichier.
     */
    fun report(stats: DistributionStats): String {
        val proposed = WorldDistribution.proposeThresholds(stats, DistributionTargets.presenceByElement)
        return buildString {
            appendLine("== Distribution du monde mesurée ==")
            appendLine("Échantillon : ${stats.totalTiles} tuiles")
            appendLine()
            appendLine("Présence par élément (cible → mesuré, écart) :")
            Element.entries.forEach { appendLine(presenceLine(it, stats)) }
            appendLine()
            appendLine(averageLine(stats))
            appendLine(emptyTilesLine(stats))
            appendLine()
            appendLine("Seuils proposés (pour atteindre les cibles ; constantes inchangées) :")
            Element.entries.forEach { appendLine(proposedLine(it, proposed)) }
        }
    }

    /** Ligne « Élément : cible → mesuré (écart) » pour la présence. */
    private fun presenceLine(element: Element, stats: DistributionStats): String {
        val target = DistributionTargets.presenceByElement.getValue(element)
        val measured = stats.presenceRate(element)
        return "  ${label(element)} : ${pct(target)} → ${pct(measured)}  (${deltaPoints(measured, target)})"
    }

    /** Ligne « Élément : seuil actuel → seuil proposé ». */
    private fun proposedLine(element: Element, proposed: Map<Element, Double>): String {
        val current = GameConfig.PRESENCE_THRESHOLDS[element.ordinal]
        return "  ${label(element)} : ${threshold(current)} → ${threshold(proposed.getValue(element))}"
    }

    /** Ligne du nombre moyen d'éléments par tuile (cible vs mesuré). */
    private fun averageLine(stats: DistributionStats): String =
        "Éléments par tuile : cible ${num(DistributionTargets.AVERAGE_ELEMENTS_PER_TILE)} " +
            "→ mesuré ${num(stats.averageElementsPerTile)}"

    /** Ligne de la proportion de tuiles vides (cible vs mesuré). */
    private fun emptyTilesLine(stats: DistributionStats): String =
        "Tuiles vides       : cible ${pct(DistributionTargets.EMPTY_RATE)} → mesuré ${pct(stats.emptyRate)}"

    /** Nom d'affichage capitalisé d'un élément (« CENDRITE » → « Cendrite »). */
    private fun label(element: Element): String = element.name.lowercase().replaceFirstChar { it.uppercase() }

    /** Pourcentage à une décimale, point décimal (« 55.0 % »). */
    private fun pct(fraction: Double): String = String.format(Locale.ROOT, "%.1f %%", fraction * 100)

    /** Écart en points de pourcentage, signé (« -5.0 pts »). */
    private fun deltaPoints(measured: Double, target: Double): String =
        String.format(Locale.ROOT, "%+.1f pts", (measured - target) * 100)

    /** Nombre à deux décimales, point décimal. */
    private fun num(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

    /** Seuil à trois décimales, point décimal. */
    private fun threshold(value: Double): String = String.format(Locale.ROOT, "%.3f", value)
}
