package com.hexa.map

/**
 * Configuration de présentation de la carte — **point unique** où ajuster le style, le cadrage
 * initial et les bornes de navigation.
 *
 * Distincte de [com.hexa.config.GameConfig] (équilibrage du jeu, module `:domain`) : ces valeurs
 * relèvent de la présentation Android et n'ont aucune sémantique métier.
 */
object MapConfig {
    /**
     * Style monochrome du projet, publié dans Mapbox Studio (cf. README → Prérequis). Dérivé de
     * Mapbox Standard : bâtiments réels masqués, aucun label, rues + eau + espaces verts conservés.
     */
    const val STYLE_URL: String = "mapbox://styles/pbernier/cmqcpqcwy001m01s4eumxepf8"

    /**
     * Latitude du centre par défaut au lancement (Paris). **Provisoire** : remplacé par la position
     * GPS réelle du joueur en #10 (permission de localisation + position filtrée).
     */
    const val DEFAULT_CENTER_LAT: Double = 48.8566

    /** Longitude du centre par défaut au lancement (Paris). Provisoire, cf. [DEFAULT_CENTER_LAT]. */
    const val DEFAULT_CENTER_LON: Double = 2.3522

    /** Zoom minimal autorisé au pincement — empêche de dézoomer au-delà de l'échelle de quartier. */
    const val MIN_ZOOM: Double = 14.0

    /** Zoom maximal autorisé au pincement — échelle rue, cohérente avec les hexagones H3 res-11. */
    const val MAX_ZOOM: Double = 19.0

    /** Zoom appliqué au lancement, à mi-plage de [MIN_ZOOM]–[MAX_ZOOM]. */
    const val DEFAULT_ZOOM: Double = 17.0
}
