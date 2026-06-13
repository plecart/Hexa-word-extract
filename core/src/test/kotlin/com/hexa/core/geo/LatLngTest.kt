package com.hexa.core.geo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [LatLng] est un simple type valeur : il transporte une position géographique sans logique.
 * On vérifie le contrat d'un type valeur (accès aux composantes, égalité structurelle), pas
 * davantage — la projection sur la sphère est testée dans [UnitSphereTest].
 */
class LatLngTest : StringSpec({
    "expose la latitude et la longitude fournies" {
        val position = LatLng(latDeg = 48.85, lngDeg = 2.35)
        position.latDeg shouldBe 48.85
        position.lngDeg shouldBe 2.35
    }

    "deux positions de mêmes coordonnées sont égales" {
        LatLng(48.85, 2.35) shouldBe LatLng(48.85, 2.35)
    }
})
