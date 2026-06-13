package com.hexa.world

import com.hexa.core.geo.LatLng

/**
 * Port : résout le **centre géographique** d'une tuile à partir de son index H3.
 *
 * C'est la seule frontière entre le générateur de monde — Kotlin pur, déterministe, partageable
 * serveur — et la bibliothèque de grille H3 (native, côté `:app`). En isolant `cellToLatLng`
 * derrière cette interface minimale, `:domain` reste sans dépendance native : la résolution réelle
 * est injectée (adaptateur H3 côté `:app`), et un faux suffit à tester le pipeline.
 */
fun interface TileCenterLocator {
    /**
     * @param h3Index index H3 (résolution du jeu) de la tuile.
     * @return la position du centre de la tuile, à projeter ensuite sur la sphère.
     */
    fun centerOf(h3Index: Long): LatLng
}
