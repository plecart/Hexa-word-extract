package com.hexa.location

import com.hexa.core.geo.FULL_TURN_DEGREES
import com.hexa.core.geo.wrapDegrees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce

/**
 * Lissage d'un cap (boussole) par moyenne mobile exponentielle **circulaire**.
 *
 * Un cap est un angle modulo 360° : lisser naïvement (`prev + factor·(raw − prev)`) produit un
 * à-coup au passage 359°→0° (la moyenne « repart » vers 180°). Ce module interpole le long du
 * **plus court arc** entre l'ancien cap et le brut, ce qui supprime l'à-coup. Le wrap d'angle est
 * délégué au point unique [wrapDegrees] (module `:core`).
 *
 * Sans état : [smooth] est une fonction pure ; [smoothedHeading] l'applique en cascade sur un flux.
 */
object HeadingSmoother {
    private const val HALF_TURN_DEG = FULL_TURN_DEGREES / 2.0

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
        val shortestDelta = (rawDeg - previousDeg + HALF_TURN_DEG).wrapDegrees() - HALF_TURN_DEG
        return (previousDeg + factor * shortestDelta).wrapDegrees()
    }

    /**
     * Applique [smooth] en cascade sur un flux de caps bruts : le **premier** cap sert d'amorçage,
     * chaque cap suivant est lissé contre le résultat précédent. Le flux entier est ramené dans
     * `[0, 360)` (premier cap inclus) : la postcondition tient **par construction**, sans dépendre
     * d'une source qui aurait déjà normalisé.
     *
     * @param factor coefficient de lissage, cf. [smooth].
     */
    fun Flow<Double>.smoothedHeading(factor: Double): Flow<Double> =
        map { it.wrapDegrees() }.runningReduce { smoothed, raw -> smooth(smoothed, raw, factor) }
}
