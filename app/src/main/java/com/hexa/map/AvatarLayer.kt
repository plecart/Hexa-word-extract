package com.hexa.map

import com.hexa.core.geo.LatLng
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.ModelLayer
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.model.addModel
import com.mapbox.maps.extension.style.model.model
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant la position de l'avatar (un point, ou aucun). */
private const val AVATAR_SOURCE_ID = "hexa-avatar-source"

/** Identifiant de la couche de modèle 3D rendant le `model.glb` de l'avatar sur ce point. */
private const val AVATAR_MODEL_LAYER_ID = "hexa-avatar-model-layer"

/**
 * Met à jour le **modèle 3D de l'avatar** du style à la position lissée [position].
 *
 * Au premier appel, **enregistre** le `model.glb` de l'avatar puis crée la source GeoJSON et une couche
 * de modèle : le point porte le modèle ancré au sol ([ModelType.COMMON_3D]), mis à l'échelle de
 * [MapConfig.AVATAR_MODEL_SCALE], remonté à sa **position de repos** de flottement ([avatarRestZ] :
 * ancrage au sol + hauteur de repos), et orienté vers une **direction par défaut fixe**
 * ([MapConfig.AVATAR_MODEL_FACING_DEG] : le mesh a son avant modélisé vers +X). Ensuite, ne fait que
 * **réinjecter le point** dans la source existante — bien moins coûteux que de recréer la couche, pour
 * suivre la position sans saccade quand le joueur marche.
 *
 * Calque la mécanique de [Style.showBuildingModels] (modèle enregistré une fois, données réinjectées).
 * La couche de rendu reste **séparée de la logique de position** : elle ne reçoit qu'un point. La
 * rotation dynamique (cap boussole / GPS) viendra piloter `modelRotation` dans les tranches suivantes.
 *
 * @param position position d'ancrage de l'avatar, ou `null` tant qu'aucun fix GPS n'est connu — la
 *   source est alors vide et rien n'est dessiné.
 */
internal fun Style.showAvatar(position: LatLng?) {
    val features = avatarFeatures(position)
    getSourceAs<GeoJsonSource>(AVATAR_SOURCE_ID)?.let { source ->
        source.featureCollection(features)
        return
    }
    registerAvatarModel()
    addSource(geoJsonSource(AVATAR_SOURCE_ID) { featureCollection(features) })
    if (getLayer(AVATAR_MODEL_LAYER_ID) == null) {
        val scale = MapConfig.AVATAR_MODEL_SCALE
        addLayer(
            modelLayer(AVATAR_MODEL_LAYER_ID, AVATAR_SOURCE_ID) {
                modelId(MapConfig.AVATAR_MODEL_GLB)
                modelType(ModelType.COMMON_3D)
                modelScale(listOf(scale, scale, scale))
                // Avant du mesh modélisé vers +X : rotation par défaut (lacet) pour l'aligner sur la
                // direction de référence. Les tranches d'orientation piloteront ce lacet dynamiquement.
                modelRotation(listOf(0.0, 0.0, MapConfig.AVATAR_MODEL_FACING_DEG))
                // Position de repos du flottement (+Z = vers le haut) : base posée au sol (demi-hauteur)
                // puis remontée de la hauteur de repos. Le driver de frame (cf. MapScreen) ré-écrit
                // ensuite ce Z à chaque frame pour l'oscillation ; il sert ici de valeur initiale.
                modelTranslation(listOf(0.0, 0.0, avatarRestZ()))
            },
        )
    }
}

/**
 * Applique le **flottement vertical** courant [offsetM] (en mètres) au modèle de l'avatar, en
 * ré-écrivant la seule composante Z de `modelTranslation` de la couche existante :
 * `translationZ = `[avatarRestZ]` + offsetM` (position de repos détachée du sol + oscillation).
 *
 * Conçu pour un appel **par frame** par le driver d'animation (cf. [com.hexa.map.MapScreen]) : ne
 * touche ni la source, ni la position GPS, ni l'orientation — uniquement le décalage vertical, ajouté
 * par-dessus le rendu posé par [showAvatar]. Sans effet tant que la couche n'existe pas (avatar pas
 * encore affiché, faute de fix GPS) : l'appel est alors un no-op silencieux.
 *
 * @param offsetM décalage de flottement à appliquer, en mètres (cf. [avatarFloatOffsetMeters]).
 */
internal fun Style.floatAvatar(offsetM: Double) {
    getLayerAs<ModelLayer>(AVATAR_MODEL_LAYER_ID)
        ?.modelTranslation(listOf(0.0, 0.0, avatarRestZ() + offsetM))
}

/**
 * Z de la **position de repos** du modèle de l'avatar, en mètres au-dessus du terrain : ancrage au sol
 * ([MapConfig.AVATAR_MODEL_GROUND_LIFT_M]) plus la hauteur de lévitation
 * ([MapConfig.AVATAR_FLOAT_REST_HEIGHT_M]). C'est le centre autour duquel l'oscillation de flottement
 * joue (offset nul) ; la garantir ≥ amplitude empêche le modèle de passer sous le sol au bas du cycle.
 */
private fun avatarRestZ(): Double = MapConfig.AVATAR_MODEL_GROUND_LIFT_M + MapConfig.AVATAR_FLOAT_REST_HEIGHT_M

/**
 * Enregistre dans le style le `model.glb` de l'avatar sous son chemin d'asset comme identifiant de
 * modèle, de sorte que la couche [showAvatar] puisse le référencer.
 */
@OptIn(MapboxExperimental::class)
private fun Style.registerAvatarModel() {
    addModel(model(MapConfig.AVATAR_MODEL_GLB) { uri("asset://${MapConfig.AVATAR_MODEL_GLB}") })
}

/**
 * Convertit la position en collection GeoJSON : un point unique d'ancrage du modèle, ou une collection
 * vide quand la position est inconnue.
 */
private fun avatarFeatures(position: LatLng?): FeatureCollection {
    val point = position?.let { Point.fromLngLat(it.lngDeg, it.latDeg) }
        ?: return FeatureCollection.fromFeatures(emptyList())
    return FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(point)))
}
