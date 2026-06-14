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

    /**
     * Inclinaison de la caméra de poursuite, en degrés (vue troisième personne). Choisie dans la
     * plage 55–65° du game design : assez rasante pour donner du relief, sans masquer l'avant.
     */
    const val PITCH: Double = 60.0

    /**
     * Zoom maintenu en poursuite tant que l'utilisateur n'a pas pincé. Légèrement plus rapproché que
     * [DEFAULT_ZOOM] pour cadrer ~150–300 m autour du joueur, en restant dans [MIN_ZOOM]–[MAX_ZOOM].
     */
    const val FOLLOW_ZOOM: Double = 17.5

    /**
     * Coefficient de lissage du cap de la boussole, dans `]0, 1]` (cf.
     * [HeadingSmoother][com.hexa.location.HeadingSmoother]). Faible = caméra douce mais réactive
     * avec retard ; élevé = réactive mais nerveuse. 0,15 amortit le bruit du capteur sans traîner.
     */
    const val HEADING_SMOOTHING_FACTOR: Double = 0.15

    /**
     * Coefficient de lissage de la position GPS, dans `]0, 1]` (cf.
     * [PositionFilter][com.hexa.location.PositionFilter]). Le GPS rafraîchit ~1 Hz : 0,25 stabilise
     * le tremblement à l'arrêt tout en suivant la marche sans retard perceptible.
     */
    const val POSITION_SMOOTHING_FACTOR: Double = 0.25

    /**
     * Précision horizontale maximale acceptée d'un point GPS, en mètres. Au-delà, le point est rejeté
     * par [PositionFilter][com.hexa.location.PositionFilter] (évite les sauts en environnement urbain
     * dense). 30 m laisse passer un fix piéton correct sans gober les positions aberrantes.
     */
    const val ACCURACY_THRESHOLD_M: Double = 30.0

    /** Intervalle souhaité entre deux mises à jour GPS, en millisecondes (cadence piétonne). */
    const val GPS_INTERVAL_MS: Long = 1_000L
}
