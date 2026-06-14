package com.hexa.location

import com.hexa.core.geo.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * [PositionSource] de démonstration : rejoue un **trajet factice** prédéfini, un point à la fois,
 * comme si le joueur s'y déplaçait. Sert à démontrer la caméra de poursuite tant que le GPS réel
 * n'existe pas (#10) ; il sera alors remplacé par une source GPS derrière le même [PositionSource].
 *
 * @property trajectory points du trajet, émis dans l'ordre ; non vide.
 * @property step délai entre deux points consécutifs ; strictement positif.
 * @property loop si `true`, le trajet reprend au début après le dernier point (boucle infinie),
 *   utile pour une démo continue ; si `false`, le flux se termine après le dernier point.
 * @throws IllegalArgumentException si [trajectory] est vide ou si [step] n'est pas strictement
 *   positif.
 */
class SimulatedTrajectoryPositionSource(
    private val trajectory: List<LatLng>,
    private val step: Duration,
    private val loop: Boolean = true,
) : PositionSource {
    init {
        require(trajectory.isNotEmpty()) { "Le trajet simulé ne peut pas être vide." }
        require(step > Duration.ZERO) { "Le pas doit être strictement positif : $step" }
    }

    override fun positions(): Flow<LatLng> = flow {
        // Délai avant chaque point sauf le tout premier : cadence régulière, sans attente
        // initiale ni traîne finale, et continuité du rythme au rebouclage.
        var isFirst = true
        do {
            for (point in trajectory) {
                if (!isFirst) delay(step)
                isFirst = false
                emit(point)
            }
        } while (loop)
    }
}
