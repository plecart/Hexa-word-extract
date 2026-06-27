package com.hexa.core.geo

import kotlin.math.PI

/**
 * Facteur de conversion **degrés → radians** (`π / 180`), partagé par les conversions d'angles du
 * package `geo` ([GreatCircle], [UnitSphere]). Une seule définition fait foi, au lieu d'une copie
 * `private` par fichier qui pourrait diverger.
 */
internal const val DEGREES_TO_RADIANS = PI / 180.0
