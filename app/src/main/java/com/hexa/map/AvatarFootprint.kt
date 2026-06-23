package com.hexa.map

import com.hexa.core.geo.LatLng
import kotlin.math.PI
import kotlin.math.cos

/**
 * Longueur d'un degré de latitude, en mètres, sur la sphère de référence (WGS84) — la même que
 * [com.hexa.core.geo.GreatCircle]. Un degré de latitude couvre une distance constante ; un degré de
 * longitude la même distance **multipliée par le cosinus de la latitude** (les méridiens se resserrent
 * vers les pôles). Aux quelques mètres de l'empreinte de l'avatar, le modèle sphérique suffit.
 */
private const val METERS_PER_DEGREE_LAT = 2.0 * PI * 6_371_008.8 / 360.0

private const val DEGREES_TO_RADIANS = PI / 180.0

/**
 * Empreinte au sol carrée de l'avatar, centrée sur la position lissée [center], de côté [sizeMeters],
 * alignée nord/est. C'est la base que la couche d'extrusion ([Style.showAvatar]) referme puis élève en
 * cube ; la séparer en transformation **pure** (sans Mapbox ni Android) la rend vérifiable sans rendu.
 *
 * Les quatre coins sont rendus dans l'ordre **SW, SE, NE, NW** : un anneau parcouru dans le sens direct
 * (les coins consécutifs forment les côtés, les coins opposés `[0]`/`[2]` et `[1]`/`[3]` les diagonales).
 * La conversion mètres → degrés est sphérique : la latitude à pas constant, la longitude corrigée par le
 * cosinus de la latitude. Une demi-arête est appliquée de part et d'autre du centre.
 *
 * @param center position d'ancrage de l'avatar (position GPS lissée et partagée).
 * @param sizeMeters côté du carré en mètres (cf. [MapConfig.AVATAR_SIZE_M]) ; supposé positif.
 * @return les quatre coins de l'empreinte, anneau **non refermé** (la couche de rendu ajoute le retour
 *   au premier point, à l'image de [Style.showHexGrid]).
 */
fun avatarFootprint(center: LatLng, sizeMeters: Double): List<LatLng> {
    val halfSide = sizeMeters / 2.0
    val deltaLat = halfSide / METERS_PER_DEGREE_LAT
    val deltaLng = halfSide / (METERS_PER_DEGREE_LAT * cos(center.latDeg * DEGREES_TO_RADIANS))
    val south = center.latDeg - deltaLat
    val north = center.latDeg + deltaLat
    val west = center.lngDeg - deltaLng
    val east = center.lngDeg + deltaLng
    return listOf(
        LatLng(latDeg = south, lngDeg = west),
        LatLng(latDeg = south, lngDeg = east),
        LatLng(latDeg = north, lngDeg = east),
        LatLng(latDeg = north, lngDeg = west),
    )
}
