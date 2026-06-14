package com.hexa.location

import com.hexa.core.geo.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce

/**
 * Filtrage d'un flux de positions GPS brutes pour alimenter la caméra de poursuite sans tremblement.
 *
 * Deux traitements, tous deux purs (aucune dépendance Android) :
 * - **Rejet** des [PositionSample] dont la précision dépasse un seuil (points GPS peu fiables).
 * - **Lissage** par moyenne mobile exponentielle, indépendamment sur la latitude et la longitude.
 *
 * Pendant de [HeadingSmoother] côté position : [smooth] est la fonction pure, [filteredPositions]
 * l'applique en cascade sur un flux. Le lissage par axe suppose des déplacements locaux (pas de
 * franchissement de l'antiméridien ±180°), hypothèse valide pour un joueur à pied.
 */
object PositionFilter {
    /**
     * Rapproche [previous] de [sample] d'une fraction [factor] sur chaque axe (lat/lng).
     *
     * @param previous position lissée précédente.
     * @param sample nouvelle position mesurée.
     * @param factor coefficient de lissage dans `[0, 1]` : `0` fige sur l'ancienne position, `1`
     *   adopte la mesure, les valeurs intermédiaires amortissent d'autant plus que `factor` est petit.
     * @return la position lissée.
     * @throws IllegalArgumentException si [factor] est hors de `[0, 1]`.
     */
    fun smooth(previous: LatLng, sample: LatLng, factor: Double): LatLng {
        require(factor in 0.0..1.0) { "Le coefficient de lissage doit être dans [0, 1] : $factor" }
        return LatLng(
            latDeg = previous.latDeg + factor * (sample.latDeg - previous.latDeg),
            lngDeg = previous.lngDeg + factor * (sample.lngDeg - previous.lngDeg),
        )
    }

    /**
     * Rejette les mesures imprécises puis applique [smooth] en cascade : la **première** position
     * acceptée est émise telle quelle (amorçage), chaque suivante est lissée contre le résultat
     * précédent. Les points dont `accuracyM` dépasse [accuracyThresholdM] sont ignorés (ils ne
     * décalent pas le lissage).
     *
     * @param smoothingFactor coefficient de lissage, cf. [smooth].
     * @param accuracyThresholdM précision maximale acceptée, en mètres ; strictement positive.
     * @throws IllegalArgumentException si [accuracyThresholdM] n'est pas strictement positif.
     */
    fun Flow<PositionSample>.filteredPositions(smoothingFactor: Double, accuracyThresholdM: Double): Flow<LatLng> {
        require(accuracyThresholdM > 0.0) {
            "Le seuil de précision doit être strictement positif : $accuracyThresholdM"
        }
        return filter { it.accuracyM <= accuracyThresholdM }
            .map { it.position }
            .runningReduce { smoothed, raw -> smooth(smoothed, raw, smoothingFactor) }
    }
}
