package com.hexa.location

import com.hexa.core.geo.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Source d'un flux de positions suivies.
 *
 * Port qui découple la caméra de poursuite de l'**origine** des positions : en production, le GPS réel
 * de l'appareil alimente ce contrat, décoré par le partage de position chaud ([SharedPositionSource]).
 * Changer d'origine ne touche ni au contrôleur ni à son câblage — il suffit d'une autre implémentation
 * de ce contrat (par exemple une source factice dans les tests).
 */
fun interface PositionSource {
    /** Émet la position suivie au fil du temps. */
    fun positions(): Flow<LatLng>
}
