package com.hexa.map

import androidx.compose.ui.graphics.Color
import com.hexa.core.geo.LatLng
import com.hexa.player.PlacedBuilding
import com.hexa.ui.theme.ObjectAssets
import kotlin.math.roundToInt

/**
 * Placement d'un bâtiment posé, prêt pour la couche de modèles 3D Mapbox ([Style.showBuildingModels]) :
 * **où** poser le modèle ([center], centre de la tuile), **quel** modèle ([modelId], chemin du
 * `model.glb` dans `assets/`) et de **quelle teinte** ([colorHex], couleur d'identité du type).
 *
 * Pur — aucune dépendance Mapbox ni Android : c'est la frontière stable que le passage ultérieur à un
 * autre rendu n'aura pas à toucher.
 *
 * @property center centre de la tuile portant le bâtiment (ancrage du modèle).
 * @property modelId chemin du modèle dans `assets/` (ex. `objects/base/model.glb`), unique par type ;
 *   sert d'identifiant du modèle enregistré dans le style.
 * @property colorHex teinte d'identité du type au format `#RRGGBB` (appliquée via `model-color`).
 */
data class BuildingModelPlacement(
    val center: LatLng,
    val modelId: String,
    val colorHex: String,
)

/**
 * Transforme les bâtiments posés en placements de modèles 3D. Pure et testable : la résolution du
 * centre d'une tuile (H3, natif) est **injectée** via [centerOf], si bien que les mappings
 * cellule → point, type → modèle et type → couleur se vérifient sans charger H3 ni Mapbox.
 *
 * @param buildings bâtiments posés (cf. [com.hexa.player.BuildingsRepository.observe]).
 * @param centerOf résout l'index H3 textuel d'une tuile en son centre (lat/lng).
 * @return un placement par bâtiment, dans le même ordre.
 */
fun buildingPlacements(buildings: List<PlacedBuilding>, centerOf: (String) -> LatLng): List<BuildingModelPlacement> =
    buildings.map { building ->
        val asset = ObjectAssets.of(building.type)
        BuildingModelPlacement(
            center = centerOf(building.cell),
            modelId = asset.glb,
            colorHex = asset.color.toHexRgb(),
        )
    }

/** Couleur Compose → `#RRGGBB`, format attendu par la teinte de modèle Mapbox (`model-color`). */
private fun Color.toHexRgb(): String {
    fun channel(value: Float): Int = (value * 255).roundToInt()
    return "#%02X%02X%02X".format(channel(red), channel(green), channel(blue))
}
