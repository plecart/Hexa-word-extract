package com.hexa.map

import com.hexa.core.geo.LatLng
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/** Identifiant de la source GeoJSON portant l'empreinte au sol de l'avatar (un polygone, ou aucun). */
private const val AVATAR_SOURCE_ID = "hexa-avatar-source"

/** Identifiant de la couche d'extrusion qui élève l'empreinte en cube. */
private const val AVATAR_LAYER_ID = "hexa-avatar-layer"

/**
 * Met à jour le **cube placeholder de l'avatar** du style à la position lissée [position].
 *
 * Au premier appel, crée la source GeoJSON puis une couche d'extrusion qui élève l'empreinte au sol en
 * cube de [MapConfig.AVATAR_HEIGHT_M], teinté de [MapConfig.AVATAR_COLOR]. Ensuite, ne fait que
 * **réinjecter le polygone** dans la source existante — bien moins coûteux que de recréer la couche,
 * pour suivre la position sans saccade quand le joueur marche.
 *
 * Calque la mécanique de [Style.showHexGrid] et [Style.showBuildingModels] (source créée une fois,
 * données réinjectées). La couche de rendu reste **séparée de la logique de position** : elle ne reçoit
 * qu'un point, l'empreinte étant calculée par la transformation pure [avatarFootprint].
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
    addSource(geoJsonSource(AVATAR_SOURCE_ID) { featureCollection(features) })
    if (getLayer(AVATAR_LAYER_ID) == null) {
        addLayer(
            fillExtrusionLayer(AVATAR_LAYER_ID, AVATAR_SOURCE_ID) {
                // Base au sol (défaut 0) → cube extrudé jusqu'à sa hauteur.
                fillExtrusionColor(MapConfig.AVATAR_COLOR)
                fillExtrusionHeight(MapConfig.AVATAR_HEIGHT_M)
            },
        )
    }
}

/**
 * Convertit la position en collection GeoJSON : l'empreinte carrée ([avatarFootprint]) refermée en
 * polygone, ou une collection vide quand la position est inconnue.
 */
private fun avatarFeatures(position: LatLng?): FeatureCollection {
    val corners = position?.let { avatarFootprint(it, MapConfig.AVATAR_SIZE_M) }
        ?: return FeatureCollection.fromFeatures(emptyList())
    val ring = corners.map { Point.fromLngLat(it.lngDeg, it.latDeg) }
    val closedRing = ring + ring.first()
    return FeatureCollection.fromFeatures(
        listOf(Feature.fromGeometry(Polygon.fromLngLats(listOf(closedRing)))),
    )
}
