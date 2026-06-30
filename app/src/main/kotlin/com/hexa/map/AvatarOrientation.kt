package com.hexa.map

import com.hexa.core.geo.wrapDegrees

/**
 * Lacet (rotation autour de +Z, en degrés) à appliquer au `modelRotation` du modèle de l'avatar pour
 * l'orienter selon le **cap boussole** [headingDeg], **composé** avec le calage du mesh
 * [facingOffsetDeg].
 *
 * Le mesh a son avant modélisé vers +X ; [facingOffsetDeg] (cf. [MapConfig.AVATAR_MODEL_FACING_DEG])
 * est le lacet qui aligne cet avant sur la direction de référence. Le cap dynamique se **compose**
 * avec ce calage plutôt que de l'écraser : à cap nul le lacet vaut exactement le calage (baseline
 * préservée), et tourner le cap d'un angle tourne le lacet du même angle. Le résultat est normalisé
 * dans `[0, 360)` via le point de wrap unique [wrapDegrees] (module `:core`), si bien qu'un cap brut
 * hors plage donne le même lacet que son équivalent dans le tour.
 *
 * Fonction **pure** : aucune dépendance Mapbox/Android, donc directement testable hors device. Le
 * lissage du cap est traité en amont (opérateur de flux
 * [smoothedHeading][com.hexa.location.HeadingSmoother.smoothedHeading]) ; cette fonction n'applique
 * que la composition géométrique. Le **signe** de la rotation et la valeur du calage relèvent de la
 * validation visuelle sur device (conventions `modelRotation` Mapbox vs azimut boussole).
 *
 * @param headingDeg cap boussole lissé, en degrés (idéalement `[0, 360)`, mais toute valeur est
 *   acceptée et ramenée dans le tour).
 * @param facingOffsetDeg calage du mesh, en degrés (cf. [MapConfig.AVATAR_MODEL_FACING_DEG]).
 * @return le lacet à écrire sur la composante Z de `modelRotation`, normalisé dans `[0, 360)`.
 */
internal fun avatarModelYawDeg(headingDeg: Double, facingOffsetDeg: Double): Double =
    (facingOffsetDeg + headingDeg).wrapDegrees()
