package com.hexa.core.geo

/**
 * Point en coordonnées cartésiennes 3D.
 *
 * Sert de sortie à [UnitSphere] et d'entrée d'échantillonnage pour le bruit procédural : les trois
 * composantes alimentent directement une fonction de bruit 3D. Type de données pur, sans logique.
 *
 * @property x composante selon l'axe X.
 * @property y composante selon l'axe Y.
 * @property z composante selon l'axe Z.
 */
data class Vector3(val x: Double, val y: Double, val z: Double)
