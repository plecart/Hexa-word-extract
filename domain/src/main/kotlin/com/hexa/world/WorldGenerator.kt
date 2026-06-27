package com.hexa.world

import com.hexa.config.Element
import com.hexa.config.GameConfig
import com.hexa.core.geo.UnitSphere
import com.hexa.core.geo.Vector3
import com.hexa.core.noise.SimplexNoise3D
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Générateur de monde procédural — **module profond** dont toute la richesse tient derrière un seul
 * appel : [contentOf] (index H3 → contenu de tuile).
 *
 * Pour chaque élément, le contenu est recalculé à la volée — jamais stocké — par un pipeline
 * strictement déterministe (game design §3.2) :
 * ```
 * centre de tuile (port)         → (lat, lng)
 *   → vecteur unitaire sphère     (UnitSphere ; ni couture à l'antiméridien, ni distorsion polaire)
 *   → bruit simplex 3D à 2 octaves (fréquence et offset propres à l'élément, seed global)
 *   → normalisation [0, 1]
 *   → seuil de présence           (absent en dessous)
 *   → richesse = (v − seuil) / (1 − seuil)                     ∈ ]0, 1]
 *   → vitesse  = arrondi(taux_base × (RATE_FLOOR + (1−RATE_FLOOR) × richesse))
 * ```
 * Toutes les constantes d'équilibrage viennent de [GameConfig] : un seul réglage s'y propage.
 *
 * Le même index donne toujours le même contenu, sur tout appareil — condition du futur multijoueur.
 * Aucune dépendance Android ni native : la grille H3 est isolée derrière [TileCenterLocator].
 *
 * @param centerLocator résout le centre géographique d'une tuile à partir de son index H3.
 */
class WorldGenerator(private val centerLocator: TileCenterLocator) {
    /** Champ de bruit unique, partagé par tous les éléments ; leur indépendance vient des offsets. */
    private val noise = SimplexNoise3D(GameConfig.WORLD_SEED)

    /**
     * Calcule le contenu de la tuile d'index [h3Index].
     *
     * @return les éléments présents sur la tuile (par rareté croissante), avec richesse et vitesse ;
     *   liste vide si la tuile ne contient rien.
     */
    fun contentOf(h3Index: Long): TileContent {
        val center = centerLocator.centerOf(h3Index)
        val point = UnitSphere.fromLatLng(center.latDeg, center.lngDeg)
        return TileContent(Element.entries.mapNotNull { depositFor(it, point) })
    }

    /** Gisement de [element] au point [point] de la sphère, ou `null` si l'élément est absent. */
    private fun depositFor(element: Element, point: Vector3): ElementDeposit? {
        val threshold = GameConfig.PRESENCE_THRESHOLDS[element.ordinal]
        val presence = presenceValue(element, point)
        if (presence <= threshold) return null

        val richness = (presence - threshold) / (1.0 - threshold)
        val base = GameConfig.BASE_RATES_PER_HOUR[element.ordinal]
        val rate = (base * (GameConfig.RATE_FLOOR + RICHNESS_WEIGHT * richness)).roundToInt()
        return ElementDeposit(element, richness, rate)
    }

    /**
     * Valeur de présence normalisée dans `[0, 1]` du champ de [element] au point [point].
     *
     * Le champ est propre à l'élément : sa **fréquence** dérive de sa longueur d'onde (clusters plus
     * ou moins vastes) et son **offset** déplace l'échantillonnage dans une région distincte du bruit
     * — ainsi les cinq champs sont indépendants et leurs gisements ne se superposent pas. Une seconde
     * octave (amplitude et multiplicateur de [GameConfig]) casse la régularité des contours.
     */
    private fun presenceValue(element: Element, point: Vector3): Double {
        val frequency = EARTH_CIRCUMFERENCE_M / GameConfig.WAVELENGTHS_M[element.ordinal]
        val offset = element.ordinal * FIELD_OFFSET_SPACING
        val x = point.x * frequency + offset
        val y = point.y * frequency + offset
        val z = point.z * frequency + offset

        val mult = GameConfig.OCTAVE2_FREQ_MULT
        val amplitude = GameConfig.OCTAVE2_AMPLITUDE
        val octaves = noise.noise(x, y, z) + amplitude * noise.noise(x * mult, y * mult, z * mult)
        val normalized = octaves / (1.0 + amplitude) // ramène la somme des deux octaves dans [-1, 1]
        return ((normalized + 1.0) / 2.0).coerceIn(0.0, 1.0)
    }

    private companion object {
        /**
         * Rayon terrestre moyen, en mètres — base du calcul de circonférence.
         *
         * Sert **uniquement de facteur d'échelle** au bruit procédural (`fréquence = circonférence /
         * longueur d'onde`) : la circonférence fixe la taille des clusters, elle n'est jamais une
         * mesure au sol. La précision du rayon est donc **sans objet** ici, d'où une valeur ronde,
         * **volontairement indépendante** du rayon WGS84 (`6_371_008.8`) qu'emploie la géodésie réelle
         * ([com.hexa.core.geo.GreatCircle]). Aligner les deux couplerait à tort le paramétrage du
         * bruit à la constante géodésique.
         */
        const val EARTH_RADIUS_M = 6_371_000.0

        /** Circonférence terrestre, en mètres : `fréquence = circonférence / longueur d'onde`. */
        val EARTH_CIRCUMFERENCE_M = 2.0 * PI * EARTH_RADIUS_M

        /** Décalage d'échantillonnage entre éléments successifs : rend les cinq champs indépendants. */
        const val FIELD_OFFSET_SPACING = 1000.0

        /** Poids de la richesse dans la vitesse : le complément du plancher (`1 − RATE_FLOOR`). */
        const val RICHNESS_WEIGHT = 1.0 - GameConfig.RATE_FLOOR
    }
}
