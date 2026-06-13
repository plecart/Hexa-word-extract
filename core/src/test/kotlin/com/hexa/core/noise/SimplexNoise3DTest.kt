package com.hexa.core.noise

import com.hexa.core.geo.UnitSphere
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs

/**
 * Échantillonne le champ sur un réseau de pas irrationnel (évite de ne tomber que sur les nœuds
 * du simplexe, où le bruit vaut 0) — ~10 000 points, suffisant pour des tests statistiques.
 */
private fun latticeSamples(): List<Triple<Double, Double, Double>> {
    val axis = (0 until 22).map { -13.0 + it * 1.21 }
    return buildList {
        for (x in axis) for (y in axis) for (z in axis) add(Triple(x, y, z))
    }
}

/**
 * Vérifie le contrat **observable** du bruit simplex 3D (PRD #3, user stories 10 et 13) :
 * déterminisme, bornes, couverture de l'intervalle. Jamais les valeurs internes de l'algorithme.
 */
class SimplexNoise3DTest : StringSpec({
    "même seed et mêmes coordonnées donnent toujours la même valeur" {
        val a = SimplexNoise3D(seed = 42L)
        val b = SimplexNoise3D(seed = 42L)
        latticeSamples().forEach { (x, y, z) ->
            a.noise(x, y, z) shouldBe b.noise(x, y, z)
        }
    }

    "toutes les valeurs produites restent dans [-1, 1]" {
        val noise = SimplexNoise3D(seed = 7L)
        latticeSamples().forEach { (x, y, z) ->
            val value = noise.noise(x, y, z)
            value shouldBeGreaterThan -1.0000001
            value shouldBeLessThan 1.0000001
        }
    }

    "le champ couvre largement l'intervalle et varie réellement" {
        val noise = SimplexNoise3D(seed = 7L)
        val values = latticeSamples().map { (x, y, z) -> noise.noise(x, y, z) }

        values.min() shouldBeLessThan -0.3
        values.max() shouldBeGreaterThan 0.3

        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        variance shouldBeGreaterThan 0.01
    }

    "deux seeds différents produisent des champs différents" {
        val fieldA = SimplexNoise3D(seed = 1L)
        val fieldB = SimplexNoise3D(seed = 2L)
        val samples = latticeSamples()

        val meanAbsDiff = samples.sumOf { (x, y, z) ->
            abs(fieldA.noise(x, y, z) - fieldB.noise(x, y, z))
        } / samples.size

        meanAbsDiff shouldBeGreaterThan 0.05
    }

    "deux points proches donnent des valeurs proches (continuité)" {
        val noise = SimplexNoise3D(seed = 7L)
        val step = 1e-3
        latticeSamples().forEach { (x, y, z) ->
            val delta = abs(noise.noise(x, y, z) - noise.noise(x + step, y + step, z + step))
            delta shouldBeLessThan 0.05
        }
    }

    "le champ échantillonné sur la sphère n'a pas de couture à l'antiméridien" {
        val noise = SimplexNoise3D(seed = 7L)
        // Échelle de fréquence représentative d'un champ de présence (cf. GameConfig.WAVELENGTHS_M).
        val frequency = 100.0
        fun sampleAt(lat: Double, lng: Double): Double {
            val v = UnitSphere.fromLatLng(lat, lng)
            return noise.noise(v.x * frequency, v.y * frequency, v.z * frequency)
        }
        listOf(0.0, 40.0, -55.0, 80.0).forEach { lat ->
            abs(sampleAt(lat, 179.999) - sampleAt(lat, -179.999)) shouldBeLessThan 0.05
        }
    }
})
