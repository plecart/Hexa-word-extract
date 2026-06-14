package com.hexa.map

import com.hexa.core.geo.LatLng
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Trajet factice rejoué pour **démontrer** la caméra de poursuite tant que le GPS réel n'existe pas.
 *
 * **Provisoire** : alimente [com.hexa.location.SimulatedTrajectoryPositionSource] le temps de cette
 * tranche, puis sera **supprimé** quand la position GPS filtrée (#10) deviendra la vraie source.
 * Les points décrivent une petite boucle piétonne autour du centre par défaut (Paris), espacés de
 * quelques dizaines de mètres pour un déplacement visible mais réaliste.
 */
object DemoTrajectory {
    /** Pas de temps entre deux points — cadence d'un déplacement à pied. */
    val STEP: Duration = 1.seconds

    /** Points de la boucle, rejoués dans l'ordre puis répétés. */
    val POINTS: List<LatLng> = listOf(
        LatLng(48.8566, 2.3522),
        LatLng(48.8569, 2.3525),
        LatLng(48.8572, 2.3529),
        LatLng(48.8574, 2.3534),
        LatLng(48.8573, 2.3540),
        LatLng(48.8570, 2.3543),
        LatLng(48.8567, 2.3540),
        LatLng(48.8565, 2.3535),
        LatLng(48.8564, 2.3529),
    )
}
