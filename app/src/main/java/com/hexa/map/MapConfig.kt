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
     * Latitude du centre de repli au lancement (Paris), affichée tant que le premier fix GPS n'est pas
     * arrivé ; la caméra bascule ensuite sur la position GPS réelle filtrée du joueur.
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

    /**
     * Délai de maintien des sources partagées (position GPS, tuile courante…) après la **dernière
     * désinscription**, en millisecondes — paramètre de
     * [WhileSubscribed][kotlinx.coroutines.flow.SharingStarted.WhileSubscribed]. Source de vérité unique consommée par
     * tous les flux chauds de `:app` (les ViewModels carte/premier lancement et
     * [com.hexa.HexaApplication]) : une rotation d'écran ou une navigation brève libère puis réabonne
     * l'observateur en moins de ce délai, sans relancer le GPS. 5 s couvre confortablement une
     * recréation d'activité tout en relâchant vite la source quand l'écran part vraiment.
     *
     * Le module pur `:location` ([com.hexa.location.SharedPositionSource]) ne peut pas dépendre de
     * `:app` : il garde son propre défaut, aligné par convention sur cette constante de référence.
     */
    const val SOURCE_STOP_TIMEOUT_MS: Long = 5_000L

    /**
     * Marge d'hystérésis du suivi de la tuile courante, en mètres (cf.
     * [CurrentTileTracker]). On ne bascule sur la cellule voisine que si son centre est plus proche
     * d'au moins cette marge que celui de la tuile courante : à l'arrêt en bordure, le tremblement
     * résiduel du GPS lissé ne fait alors plus clignoter la tuile. Même famille que
     * [POSITION_SMOOTHING_FACTOR]/[ACCURACY_THRESHOLD_M] : robustesse au jitter GPS. 8 m ≈ un tiers
     * d'arête de cellule res-11 (~25 m), assez pour stabiliser sans retarder un vrai changement.
     */
    const val TILE_HYSTERESIS_MARGIN_M: Double = 8.0

    /** Nombre d'anneaux minimal de la grille (zoom le plus rapproché) — 2 anneaux ≈ 19 cellules. */
    const val GRID_MIN_RINGS: Int = 2

    /** Nombre d'anneaux maximal de la grille (zoom le plus large) — 4 anneaux ≈ 61 cellules. */
    const val GRID_MAX_RINGS: Int = 4

    /**
     * Paliers de zoom → nombre d'anneaux de la grille, **ordonnés du plus zoomé au plus large**.
     * Chaque palier `(zoom plancher, anneaux)` s'applique tant que le zoom reste au-dessus du plancher.
     * Plus on dézoome, plus on affiche d'anneaux pour garder la grille pleine écran ; la grille n'est
     * recalculée que lorsque le zoom **franchit** un palier, pas à chaque micro-variation. Les trois
     * paliers couvrent : `≥ 18` échelle rue (19 cellules), `[16,5 ; 18[` (37 cellules), `[14 ; 16,5[`
     * échelle quartier (61 cellules).
     */
    val GRID_RING_STEPS: List<Pair<Double, Int>> =
        listOf(
            18.0 to GRID_MIN_RINGS,
            16.5 to 3,
            MIN_ZOOM to GRID_MAX_RINGS,
        )

    /** Couleur du tracé de la grille — cyan vif, contrasté sur le fond monochrome. **Provisoire**. */
    const val GRID_LINE_COLOR: String = "#00E5FF"

    /** Épaisseur du tracé de la grille, en points — fin mais lisible. **Provisoire**. */
    const val GRID_LINE_WIDTH: Double = 1.5

    /** Opacité du tracé de la grille, dans `[0, 1]` — surcouche visible sans masquer la carte. **Provisoire**. */
    const val GRID_LINE_OPACITY: Double = 0.85

    /**
     * Remplissage de la **tuile courante** (sous le joueur) — cyan translucide, accordé à
     * [GRID_LINE_COLOR] : surligne la cellule sans masquer la carte dessous. **Provisoire**.
     */
    const val TILE_CURRENT_FILL_COLOR: String = "rgba(0, 229, 255, 0.25)"

    /** Remplissage des **tuiles normales** — transparent : seule leur ligne de contour les dessine. */
    const val TILE_NORMAL_FILL_COLOR: String = "rgba(0, 0, 0, 0.0)"

    /**
     * Facteur d'échelle appliqué au `model.glb` d'un bâtiment posé sur la carte (couche de modèles 3D,
     * cf. [Style.showBuildingModels]). Le glb placeholder est modélisé à l'unité ; ce facteur le porte
     * à la hauteur visée (~15 m, cf. PRD #4). **À affiner à la validation visuelle sur device** une
     * fois l'art définitif en place (ancrage au sol, lisibilité caméra inclinée). **Provisoire**.
     */
    const val BUILDING_MODEL_SCALE: Double = 15.0

    /**
     * Remontée verticale du modèle, en mètres, pour **poser sa base au sol** : le `model.glb` est un
     * cube centré sur son origine (sommets de −0,5 à +0,5), donc ancré au sol il s'enterrerait de moitié.
     * On le remonte d'une demi-hauteur (= demi-[BUILDING_MODEL_SCALE], le cube étant mis à l'échelle
     * uniformément). À revoir si l'art définitif est autoré « base à l'origine » (la remontée tomberait
     * alors à 0). **Provisoire**.
     */
    const val BUILDING_MODEL_GROUND_LIFT_M: Double = BUILDING_MODEL_SCALE / 2.0

    /**
     * Intensité du mélange entre la couleur propre du modèle et sa **teinte d'identité par type**
     * (`model-color-mix-intensity`, dans `[0, 1]` : 0 = couleur du glb, 1 = teinte pleine). 0,7 teinte
     * franchement base ≠ extracteur tout en gardant un peu du modelé du glb. **Provisoire**.
     */
    const val BUILDING_MODEL_COLOR_MIX: Double = 0.7
}
