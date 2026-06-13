package com.hexa.core.geo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt

/** Norme euclidienne d'un triplet — sert à vérifier l'unitarité et la distance entre deux points. */
private fun norm(x: Double, y: Double, z: Double): Double = sqrt(x * x + y * y + z * z)

/**
 * Vérifie la conversion (lat, lng) → vecteur unitaire sur la sphère, fondation de
 * l'échantillonnage du bruit sans couture ni distorsion polaire (PRD #3, user story 13).
 *
 * On teste le **comportement observable** : valeurs aux points cardinaux, norme unitaire,
 * absence de couture à l'antiméridien et coïncidence des pôles — jamais une formule interne.
 */
class UnitSphereTest : StringSpec({
    val tolerance = 1e-12

    "le centre (0,0) pointe vers +X" {
        val v = UnitSphere.fromLatLng(latDeg = 0.0, lngDeg = 0.0)
        v.x shouldBe (1.0 plusOrMinus tolerance)
        v.y shouldBe (0.0 plusOrMinus tolerance)
        v.z shouldBe (0.0 plusOrMinus tolerance)
    }

    "la longitude +90° pointe vers +Y, -90° vers -Y" {
        UnitSphere.fromLatLng(0.0, 90.0).y shouldBe (1.0 plusOrMinus tolerance)
        UnitSphere.fromLatLng(0.0, -90.0).y shouldBe (-1.0 plusOrMinus tolerance)
    }

    "le pôle Nord pointe vers +Z, le pôle Sud vers -Z" {
        UnitSphere.fromLatLng(90.0, 0.0).z shouldBe (1.0 plusOrMinus tolerance)
        UnitSphere.fromLatLng(-90.0, 0.0).z shouldBe (-1.0 plusOrMinus tolerance)
    }

    "tout point produit un vecteur de norme 1" {
        val samples = listOf(
            0.0 to 0.0,
            45.0 to 17.0,
            -33.5 to 151.2,
            89.999 to -179.999,
            -12.0 to 200.0,
        )
        samples.forEach { (lat, lng) ->
            val v = UnitSphere.fromLatLng(lat, lng)
            norm(v.x, v.y, v.z) shouldBe (1.0 plusOrMinus tolerance)
        }
    }

    "l'antiméridien n'a pas de couture : -180° et +180° désignent le même point" {
        listOf(0.0, 37.4, -62.1).forEach { lat ->
            val west = UnitSphere.fromLatLng(lat, -180.0)
            val east = UnitSphere.fromLatLng(lat, 180.0)
            west.x shouldBe (east.x plusOrMinus tolerance)
            west.y shouldBe (east.y plusOrMinus tolerance)
            west.z shouldBe (east.z plusOrMinus tolerance)
        }
    }

    "deux points proches de part et d'autre de ±180° restent proches" {
        val west = UnitSphere.fromLatLng(10.0, 179.999)
        val east = UnitSphere.fromLatLng(10.0, -179.999)
        val gap = norm(west.x - east.x, west.y - east.y, west.z - east.z)
        gap shouldBe (0.0 plusOrMinus 1e-4)
    }

    "un pôle est un point unique : la longitude n'a aucun effet" {
        val northA = UnitSphere.fromLatLng(90.0, 0.0)
        val northB = UnitSphere.fromLatLng(90.0, 123.4)
        northA.x shouldBe (northB.x plusOrMinus tolerance)
        northA.y shouldBe (northB.y plusOrMinus tolerance)
        northA.z shouldBe (northB.z plusOrMinus tolerance)
    }
})
