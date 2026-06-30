package com.hexa.map

import com.hexa.core.geo.wrapDegrees
import kotlin.math.atan2

/**
 * Logique **pure** de la rotation de la caméra au glisser à un doigt.
 *
 * Le geste (cf. `detectDragRotation` dans `MapScreen`) ne pilote le cap que par ces deux fonctions :
 * le doigt qui **tourne autour du joueur** (centre de l'écran) « emmène » la carte, qui pivote du
 * même angle et dans le même sens — une rotation **directe (1:1)**, sans sensibilité à régler. Sans
 * dépendance Android/Compose ni état mutable : testable isolément.
 */
object CameraRotationGesture {
    /**
     * Nouveau cap quand le doigt a tourné de [startAngleDeg] à [currentAngleDeg] autour du joueur.
     *
     * On **soustrait** la variation d'angle au cap : à l'écran, un cap Mapbox croissant fait pivoter la
     * carte dans le sens **anti-horaire** ; soustraire fait donc tourner la carte dans le **même** sens
     * que le doigt (elle le suit).
     *
     * @param startBearingDeg cap au moment où la rotation s'engage (seuil franchi).
     * @param startAngleDeg angle du doigt autour du centre à cet instant (cf. [angleDeg]).
     * @param currentAngleDeg angle courant du doigt autour du centre.
     * @return le cap résultant, normalisé dans `[0, 360)` ; le wrap circulaire évite tout à-coup au
     *   franchissement 360°↔0°.
     */
    fun rotate(startBearingDeg: Double, startAngleDeg: Double, currentAngleDeg: Double): Double =
        (startBearingDeg - (currentAngleDeg - startAngleDeg)).wrapDegrees()

    /**
     * Angle (en degrés) du point ([x], [y]) autour du centre ([centerX], [centerY]) dans le repère
     * **écran** (y vers le bas) : 0° à droite, +90° vers le bas, un déplacement horaire à l'écran fait
     * **croître** l'angle. Sert à mesurer de combien le doigt a tourné autour du joueur.
     */
    fun angleDeg(centerX: Float, centerY: Float, x: Float, y: Float): Double =
        Math.toDegrees(atan2((y - centerY).toDouble(), (x - centerX).toDouble()))
}
