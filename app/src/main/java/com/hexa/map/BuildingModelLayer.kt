package com.hexa.map

import com.hexa.player.PlacedBuildingType
import com.hexa.ui.theme.ObjectAssets
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.model.addModel
import com.mapbox.maps.extension.style.model.model
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant un point par bâtiment posé (centre de tuile + habillage). */
private const val BUILDINGS_SOURCE_ID = "hexa-buildings-source"

/** Identifiant de la couche de modèles 3D rendant un `model.glb` sur chaque point. */
private const val BUILDINGS_MODEL_LAYER_ID = "hexa-buildings-model-layer"

/** Propriété GeoJSON portant l'identifiant du modèle à poser ([BuildingModelPlacement.modelId]). */
private const val MODEL_ID_PROPERTY = "modelId"

/** Propriété GeoJSON portant la teinte d'identité du bâtiment ([BuildingModelPlacement.colorHex]). */
private const val COLOR_PROPERTY = "color"

/**
 * Met à jour la couche de **modèles 3D des bâtiments posés** du style avec les [placements] courants.
 *
 * Au premier appel, **enregistre une fois pour toutes** les modèles `model.glb` de chaque type de
 * bâtiment ([PlacedBuildingType]) puis crée la source GeoJSON et une couche de modèles : chaque point
 * porte un modèle (`model-id` data-driven) teinté par sa couleur d'identité (`model-color`), ancré au
 * sol (`COMMON_3D`) et mis à l'échelle de [MapConfig.BUILDING_MODEL_SCALE]. Ensuite, ne fait que
 * **réinjecter les points** dans la source existante — bien moins coûteux que de recréer la couche,
 * pour rester fluide quand un bâtiment apparaît à l'écran.
 *
 * Calque la mécanique de [Style.showHexGrid] (source créée une fois, données réinjectées). Le passage
 * ultérieur à un autre rendu ne touchera que cette couche, pas la transformation pure
 * ([buildingPlacements]) ni le registre d'assets ([ObjectAssets]).
 *
 * @param placements un point de modèle par bâtiment posé (vide tant qu'aucun n'est posé).
 */
internal fun Style.showBuildingModels(placements: List<BuildingModelPlacement>) {
    val features = buildingFeatures(placements)
    getSourceAs<GeoJsonSource>(BUILDINGS_SOURCE_ID)?.let { source ->
        source.featureCollection(features)
        return
    }
    registerBuildingModels()
    addSource(geoJsonSource(BUILDINGS_SOURCE_ID) { featureCollection(features) })
    if (getLayer(BUILDINGS_MODEL_LAYER_ID) == null) {
        addLayer(
            modelLayer(BUILDINGS_MODEL_LAYER_ID, BUILDINGS_SOURCE_ID) {
                modelId(Expression.get(MODEL_ID_PROPERTY))
                modelType(ModelType.COMMON_3D)
                modelColor(Expression.get(COLOR_PROPERTY))
                modelColorMixIntensity(MapConfig.BUILDING_MODEL_COLOR_MIX)
                modelScale(
                    listOf(
                        MapConfig.BUILDING_MODEL_SCALE,
                        MapConfig.BUILDING_MODEL_SCALE,
                        MapConfig.BUILDING_MODEL_SCALE,
                    ),
                )
            },
        )
    }
}

/**
 * Enregistre dans le style le `model.glb` de **chaque** type de bâtiment posé, sous son chemin d'asset
 * comme identifiant de modèle. Couvre tous les types d'emblée (pas seulement ceux déjà à l'écran), si
 * bien qu'un bâtiment qui apparaît ensuite référence un modèle déjà enregistré.
 */
private fun Style.registerBuildingModels() {
    PlacedBuildingType.entries.forEach { type ->
        val modelId = ObjectAssets.of(type).glb
        addModel(model(modelId) { uri("asset://$modelId") })
    }
}

/** Convertit les placements en points GeoJSON annotés de leur modèle et de leur teinte. */
private fun buildingFeatures(placements: List<BuildingModelPlacement>): FeatureCollection =
    FeatureCollection.fromFeatures(
        placements.map { placement ->
            Feature.fromGeometry(
                Point.fromLngLat(placement.center.lngDeg, placement.center.latDeg),
            ).apply {
                addStringProperty(MODEL_ID_PROPERTY, placement.modelId)
                addStringProperty(COLOR_PROPERTY, placement.colorHex)
            }
        },
    )
