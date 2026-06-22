package com.hexa.map

import com.hexa.core.geo.GreatCircle
import com.hexa.core.geo.LatLng
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt

/**
 * Verrouille la transformation **position lissée → empreinte au sol du cube avatar** : un carré centré
 * sur la position, de côté donné en mètres, orienté nord/est. C'est la frontière pure (sans Mapbox ni
 * Android) que la couche d'extrusion ([Style.showAvatar]) se contente de poser puis d'extruder ; on la
 * vérifie ici sans rendu, à la dimension métrique près.
 */
class AvatarFootprintTest : StringSpec({
    // Tolérance d'un dixième de mètre : la conversion mètres→degrés est sphérique, pas ellipsoïdale.
    val toleranceM = 0.1

    "l'empreinte est un carré de quatre coins centré sur la position, du côté demandé" {
        val center = LatLng(latDeg = 48.8566, lngDeg = 2.3522)
        val sideM = 6.0

        val corners = avatarFootprint(center, sizeMeters = sideM)

        corners.size shouldBe 4
        // Chaque côté (coins consécutifs, anneau refermé) mesure le côté demandé.
        (corners + corners.first()).zipWithNext().forEach { (a, b) ->
            GreatCircle.distanceMeters(a, b) shouldBe (sideM plusOrMinus toleranceM)
        }
        // Le centroïde des quatre coins retombe sur la position d'ancrage.
        val centroid = LatLng(
            latDeg = corners.map { it.latDeg }.average(),
            lngDeg = corners.map { it.lngDeg }.average(),
        )
        GreatCircle.distanceMeters(centroid, center) shouldBe (0.0 plusOrMinus toleranceM)
    }

    "la diagonale du carré vaut côté × √2" {
        val center = LatLng(latDeg = 0.0, lngDeg = 0.0)
        val sideM = 10.0

        val corners = avatarFootprint(center, sizeMeters = sideM)

        // Coins opposés sur l'anneau (SW↔NE, SE↔NW) : diagonale = côté·√2.
        val expectedDiagonalM = sideM * sqrt(2.0)
        GreatCircle.distanceMeters(corners[0], corners[2]) shouldBe (expectedDiagonalM plusOrMinus toleranceM)
        GreatCircle.distanceMeters(corners[1], corners[3]) shouldBe (expectedDiagonalM plusOrMinus toleranceM)
    }
})
