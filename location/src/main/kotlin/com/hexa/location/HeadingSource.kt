package com.hexa.location

import kotlinx.coroutines.flow.Flow

/**
 * Source d'un flux de caps bruts (orientation de l'appareil), en degrés.
 *
 * Port symétrique de [PositionSource] : il découple le consommateur du cap de son **origine**. L'app
 * fournit une implémentation adossée à la boussole (capteur de vecteur de rotation) ; le lissage est
 * appliqué en aval par [HeadingSmoother], indépendant de la source. Une source simulée pourrait s'y
 * substituer en test sans toucher au reste.
 *
 * Depuis #96, le cap ne pilote plus la caméra (rotation au glisser) : ce port est conservé pour
 * l'orientation de l'avatar (#100).
 */
fun interface HeadingSource {
    /** Émet le cap brut mesuré au fil du temps, en degrés (idéalement dans `[0, 360)`). */
    fun headings(): Flow<Double>
}
