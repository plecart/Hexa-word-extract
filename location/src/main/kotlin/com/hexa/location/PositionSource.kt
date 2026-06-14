package com.hexa.location

import com.hexa.core.geo.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Source d'un flux de positions suivies.
 *
 * Port qui découple la caméra de poursuite de l'**origine** des positions : cette tranche fournit
 * un trajet simulé ([SimulatedTrajectoryPositionSource]), le GPS réel le remplacera plus tard (#10)
 * sans toucher au contrôleur ni à son câblage — il suffira d'une autre implémentation de ce contrat.
 */
fun interface PositionSource {
    /** Émet la position suivie au fil du temps. */
    fun positions(): Flow<LatLng>
}
