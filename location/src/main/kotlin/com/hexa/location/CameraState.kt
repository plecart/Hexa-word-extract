package com.hexa.location

import com.hexa.core.geo.LatLng

/**
 * Pose de caméra à la troisième personne, sortie pure de [ChaseCameraController].
 *
 * Type de données sans logique ni dépendance Mapbox : il nomme l'état que la couche de rendu
 * (`:app`) traduit en `CameraOptions` du SDK. Disposer d'un type dédié — plutôt que quatre `Double`
 * anonymes — évite d'intervertir zoom, pitch et cap au câblage.
 *
 * @property center position géographique sur laquelle la caméra est centrée.
 * @property zoomLevel niveau de zoom Mapbox (plus grand = plus rapproché), déjà borné par le
 *   contrôleur à la plage de jeu.
 * @property pitchDeg inclinaison de la caméra en degrés ; 0° = vue du dessus, valeur croissante =
 *   vue plus rasante (troisième personne).
 * @property bearingDeg cap de la caméra en degrés dans `[0, 360)` ; 0° = nord, sens horaire.
 */
data class CameraState(
    val center: LatLng,
    val zoomLevel: Double,
    val pitchDeg: Double,
    val bearingDeg: Double,
)
