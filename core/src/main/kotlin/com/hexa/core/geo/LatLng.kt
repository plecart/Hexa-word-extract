package com.hexa.core.geo

/**
 * Position géographique en degrés (latitude, longitude).
 *
 * Type de données pur, sans logique : il nomme la sortie d'une résolution de centre de tuile
 * (index H3 → centre) et l'entrée de [UnitSphere.fromLatLng], qui la projette sur la sphère.
 * Disposer d'un type dédié — plutôt qu'un couple de `Double` anonyme — évite d'inverser
 * latitude et longitude au fil des appels.
 *
 * @property latDeg latitude en degrés ; +90° = pôle Nord, −90° = pôle Sud.
 * @property lngDeg longitude en degrés ; +180° et −180° désignent le même méridien.
 */
data class LatLng(val latDeg: Double, val lngDeg: Double)
