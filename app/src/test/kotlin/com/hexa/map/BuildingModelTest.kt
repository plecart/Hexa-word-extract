package com.hexa.map

import com.hexa.core.geo.LatLng
import com.hexa.player.PlacedBuilding
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Verrouille la transformation **bâtiment posé → placement de modèle 3D** : centre résolu depuis
 * l'index H3 de la tuile (résolveur injecté, donc testable sans H3 natif), modèle et teinte choisis
 * par le type via le registre [com.hexa.ui.theme.ObjectAssets]. C'est la frontière pure entre les
 * données Firestore et la couche de rendu Mapbox.
 */
class BuildingModelTest : StringSpec({
    val now = Instant.parse("2026-06-21T10:00:00Z")

    "un bâtiment posé devient un placement centré sur sa tuile, avec son modèle et sa teinte" {
        val center = LatLng(latDeg = 48.8566, lngDeg = 2.3522)
        val building = PlacedBuilding.base("8a1fb46622dffff", now)

        val placements = buildingPlacements(listOf(building)) { cell ->
            cell shouldBe "8a1fb46622dffff"
            center
        }

        placements shouldBe listOf(
            BuildingModelPlacement(center = center, modelId = "objects/base/model.glb", colorHex = "#E0A23B"),
        )
    }

    "une liste vide ne produit aucun placement" {
        buildingPlacements(emptyList()) { error("le résolveur de centre ne doit pas être appelé") } shouldBe emptyList()
    }
})
