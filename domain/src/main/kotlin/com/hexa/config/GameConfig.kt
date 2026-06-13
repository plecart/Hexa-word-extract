package com.hexa.config

/**
 * Configuration centrale du game design — **point unique** où ajuster les constantes
 * d'équilibrage du MVP (cf. `Document/Hexa_02_Game_design_et_equilibrage.md` §6).
 *
 * Ces valeurs sont **provisoires** : elles servent à valider la boucle technique, pas à
 * équilibrer le jeu. Le vrai chantier d'équilibrage est post-MVP. Tout consommateur
 * (génération procédurale, économie, écran d'accueil) lit ces constantes ici plutôt que
 * de les redéfinir, pour qu'un seul réglage se propage partout.
 *
 * Les trois listes [WAVELENGTHS_M], [PRESENCE_THRESHOLDS] et [BASE_RATES_PER_HOUR] sont
 * **indexées par rareté** dans l'ordre de [Element] : l'indice `i` décrit `Element.entries[i]`.
 */
object GameConfig {
    /** Résolution de la grille hexagonale H3 (≈ 25 m d'arête à la résolution 11). */
    const val H3_RESOLUTION: Int = 11

    /** Graine globale du monde procédural — fixe pour garantir un monde déterministe. */
    const val WORLD_SEED: Long = 0x48455841L // "HEXA" en ASCII

    /** Longueurs d'onde du bruit de présence, en mètres, par rareté croissante. */
    val WAVELENGTHS_M: List<Int> = listOf(2000, 1500, 1000, 600, 400)

    /** Seuils de présence (probabilité au-dessus de laquelle l'élément apparaît), par rareté. */
    val PRESENCE_THRESHOLDS: List<Double> = listOf(0.45, 0.70, 0.85, 0.93, 0.97)

    /** Taux d'extraction de base, en unités par heure à richesse maximale, par rareté. */
    val BASE_RATES_PER_HOUR: List<Int> = listOf(60, 30, 14, 6, 2)

    /** Plancher de richesse appliqué à une tuile où l'élément est présent (jamais 0). */
    const val RATE_FLOOR: Double = 0.20

    /** Amplitude de la seconde octave de bruit, en fraction de la première. */
    const val OCTAVE2_AMPLITUDE: Double = 0.25

    /** Multiplicateur de fréquence de la seconde octave de bruit. */
    const val OCTAVE2_FREQ_MULT: Int = 4

    /** Coût de craft d'un extracteur, en unités par élément. */
    val RECIPE_EXTRACTEUR: Map<Element, Int> =
        mapOf(Element.CENDRITE to 100, Element.GIVRELIN to 40)

    /** Inventaire de départ d'un nouveau joueur, en unités par élément. */
    val STARTER_KIT: Map<Element, Int> =
        mapOf(Element.CENDRITE to 250, Element.GIVRELIN to 100)

    /** Intervalle de rafraîchissement de la récolte affichée, en secondes. */
    const val COLLECT_REFRESH_SECONDS: Int = 30
}
