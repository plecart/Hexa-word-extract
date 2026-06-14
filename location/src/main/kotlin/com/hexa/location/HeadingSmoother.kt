package com.hexa.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.runningReduce

/**
 * Lissage d'un cap (boussole) par moyenne mobile exponentielle **circulaire**.
 *
 * Un cap est un angle modulo 360° : lisser naïvement (`prev + factor·(raw − prev)`) produit un
 * à-coup au passage 359°→0° (la moyenne « repart » vers 180°). Ce module interpole le long du
 * **plus court arc** entre l'ancien cap et le brut, ce qui supprime l'à-coup.
 *
 * Sans état : [smooth] est une fonction pure ; [smoothedHeading] l'applique en cascade sur un flux.
 */
object HeadingSmoother {
    private const val FULL_TURN_DEG = 360.0
    private const val HALF_TURN_DEG = 180.0

    /**
     * Rapproche [previousDeg] de [rawDeg] d'une fraction [factor] le long du plus court arc.
     *
     * @param previousDeg cap lissé précédent, en degrés.
     * @param rawDeg cap brut mesuré, en degrés.
     * @param factor coefficient de lissage dans `[0, 1]` : `0` fige sur l'ancien cap, `1` adopte
     *   immédiatement le brut, les valeurs intermédiaires amortissent d'autant plus que `factor`
     *   est petit.
     * @return le cap lissé, normalisé dans `[0, 360)`.
     * @throws IllegalArgumentException si [factor] est hors de `[0, 1]`.
     */
    fun smooth(previousDeg: Double, rawDeg: Double, factor: Double): Double {
        require(factor in 0.0..1.0) { "Le coefficient de lissage doit être dans [0, 1] : $factor" }
        val shortestDelta = normalize(rawDeg - previousDeg + HALF_TURN_DEG) - HALF_TURN_DEG
        return normalize(previousDeg + factor * shortestDelta)
    }

    /**
     * Applique [smooth] en cascade sur un flux de caps bruts : le **premier** cap est émis tel quel
     * (amorçage), chaque cap suivant est lissé contre le résultat précédent. Préserve la circularité
     * comme [smooth].
     *
     * @param factor coefficient de lissage, cf. [smooth].
     */
    fun Flow<Double>.smoothedHeading(factor: Double): Flow<Double> =
        runningReduce { smoothed, raw -> smooth(smoothed, raw, factor) }

    /** Ramène un angle en degrés dans `[0, 360)`, gérant les valeurs négatives. */
    private fun normalize(angleDeg: Double): Double = ((angleDeg % FULL_TURN_DEG) + FULL_TURN_DEG) % FULL_TURN_DEG
}
