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

    /**
     * Zoom minimal autorisé au pincement — borne de **dézoom** : empêche de s'éloigner au-delà de
     * ~2 rues autour du joueur (échelle de jeu serrée), plutôt que l'échelle de quartier.
     */
    const val MIN_ZOOM: Double = 17.0

    /** Zoom maximal autorisé au pincement — échelle rue, cohérente avec les hexagones H3 res-10. */
    const val MAX_ZOOM: Double = 19.0

    /** Zoom appliqué au lancement, à mi-plage de [MIN_ZOOM]–[MAX_ZOOM]. */
    const val DEFAULT_ZOOM: Double = 18.0

    /**
     * Inclinaison **minimale** de la caméra de poursuite, en degrés — appliquée au zoom le plus large
     * ([MIN_ZOOM]). Pitch le plus faible : vue la plus **en surplomb** (plongeante, sans aller jusqu'au
     * top-down) pour lire la grille hexagonale et se repérer. Le pitch est **couplé au zoom** par
     * [ChaseCameraController][com.hexa.location.ChaseCameraController], qui interpole linéairement
     * jusqu'à [MAX_PITCH]. **À affiner à la validation terrain.** **Provisoire.**
     */
    const val MIN_PITCH: Double = 40.0

    /**
     * Inclinaison **maximale** de la caméra de poursuite, en degrés — appliquée au zoom le plus
     * rapproché ([MAX_ZOOM]). Pitch élevé, caméra redressée vers l'horizon : on voit le décor et la
     * profondeur. Borne haute du couplage pitch↔zoom (cf. [MIN_PITCH]). **À affiner à la validation
     * terrain.** **Provisoire.**
     */
    const val MAX_PITCH: Double = 72.0

    /**
     * Zoom maintenu en poursuite tant que l'utilisateur n'a pas pincé. Resserré au point de vue de jeu
     * (retour de validation de #22 : la carte était trop dézoomée) une fois l'avatar posé : cadre de
     * près le joueur et sa tuile, en gardant une marge de pincement jusqu'à [MAX_ZOOM]. **À affiner à
     * la validation terrain.**
     */
    const val FOLLOW_ZOOM: Double = 18.5

    /**
     * Coefficient de lissage du cap de la boussole, dans `]0, 1]` (cf.
     * [HeadingSmoother][com.hexa.location.HeadingSmoother]). Faible = mouvement doux mais réactif
     * avec retard ; élevé = réactif mais nerveux. 0,15 amortit le bruit du capteur sans traîner.
     *
     * **Plus consommé par la caméra** depuis #96 (le pilotage boussole du cap caméra a été retiré) :
     * consommé désormais par l'**orientation de l'avatar** (#100), qui réutilise le même pipeline boussole.
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
     * [POSITION_SMOOTHING_FACTOR]/[ACCURACY_THRESHOLD_M] : robustesse au jitter GPS. Calée sur
     * l'amplitude **physique** du jitter lissé (quelques mètres), indépendante de la taille de tuile :
     * 8 m couvre le tremblement sans retarder un vrai changement. Sur une arête res-10 (~66 m), cela
     * ne pèse plus que ~1/8 d'arête (vs ~1/3 en res-11) — d'autant plus réactif que les tuiles sont
     * larges, sans rien perdre en stabilité.
     */
    const val TILE_HYSTERESIS_MARGIN_M: Double = 8.0

    /**
     * Rayon **fixe** de la grille rendue, en anneaux H3 autour de la tuile courante (centre inclus) :
     * 5 anneaux = un disque de 91 cellules (diamètre 11 hexagones), **indépendant du zoom**. Remplace
     * les anciens paliers zoom → anneaux : la grille couvre toujours la même étendue autour du joueur,
     * et c'est le **fondu avec la distance** ([GridFade]) qui efface les tuiles lointaines pour ne pas
     * surcharger la carte. **À affiner à la validation terrain.**
     */
    const val GRID_RENDER_RINGS: Int = 5

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

    /** Chemin, dans `assets/`, du `model.glb` de l'avatar (rendu par [Style.showAvatar]). */
    const val AVATAR_MODEL_GLB: String = "objects/avatar/model.glb"

    /**
     * Facteur d'échelle du modèle 3D de l'avatar. Plus petit qu'un bâtiment ([BUILDING_MODEL_SCALE])
     * pour rester à l'échelle d'un joueur tout en restant lisible à la caméra inclinée. **À affiner à
     * la validation terrain.** **Provisoire**.
     */
    const val AVATAR_MODEL_SCALE: Double = 12.0

    /**
     * Remontée verticale, en mètres, pour poser la base du modèle au sol — le modèle étant centré sur
     * son origine, sa moitié basse s'enterrerait sinon (= demi-[AVATAR_MODEL_SCALE]). **À ajuster sur
     * device** si le glb est modélisé base-à-l'origine (mettre alors `0.0`). **Provisoire**.
     */
    const val AVATAR_MODEL_GROUND_LIFT_M: Double = AVATAR_MODEL_SCALE / 2.0

    /**
     * Lacet (rotation autour de +Z, en degrés) appliqué par défaut au modèle de l'avatar. Le mesh a son
     * avant modélisé vers **+X** ; ce **calage** l'aligne sur la direction de référence. Le cap boussole
     * lissé se **compose** ensuite avec ce calage pour orienter l'avatar dynamiquement (#100, cf.
     * [avatarModelYawDeg]) ; le cap GPS viendra plus tard. **À valider sur device** (calage et signe de
     * la rotation). **Provisoire**.
     */
    const val AVATAR_MODEL_FACING_DEG: Double = 90.0

    /**
     * Hauteur de **repos** du flottement, en mètres : décalage vertical permanent ajouté **au-dessus**
     * de l'ancrage au sol ([AVATAR_MODEL_GROUND_LIFT_M]) autour duquel l'avatar oscille (effet fantôme,
     * cf. [avatarFloatOffsetMeters] / [Style.floatAvatar]). Maintenue **≥ [AVATAR_FLOAT_AMPLITUDE_M]**
     * pour qu'au bas du cycle (offset = −amplitude) le modèle ne s'enfonce pas sous le sol. **À affiner
     * à la validation terrain.** **Provisoire**.
     */
    const val AVATAR_FLOAT_REST_HEIGHT_M: Double = 0.7

    /**
     * Amplitude du flottement, en mètres : l'avatar oscille de ±cette valeur autour de sa position de
     * repos ([AVATAR_FLOAT_REST_HEIGHT_M]). Faible devant la taille du modèle ([AVATAR_MODEL_SCALE])
     * pour que l'effet « respire » sans sauter — perceptible mais discret. **À affiner à la validation
     * terrain.** **Provisoire**.
     */
    const val AVATAR_FLOAT_AMPLITUDE_M: Double = 0.6

    /**
     * Période du flottement, en millisecondes : durée d'un cycle complet montée→descente. Lente pour
     * une lévitation paisible (l'avatar « respire »), pas une oscillation nerveuse. **À affiner à la
     * validation terrain.** **Provisoire**.
     */
    const val AVATAR_FLOAT_PERIOD_MS: Long = 2_800L
}
