package com.hexa.startup

import com.hexa.core.geo.LatLng
import com.hexa.player.PlayerUiState

/**
 * État de la **machine à états de démarrage**, sélectionné par [startupStage] et rendu par la racine
 * de l'UI ([com.hexa.MainActivity]).
 */
enum class StartupStage {
    /** Position du joueur inconnue : écran de chargement plein écran (cf. [com.hexa.startup.LoadingScreen]). */
    LOADING,

    /** Position connue mais base non posée : écran de premier lancement (cf. [com.hexa.firstlaunch.FirstLaunchScreen]). */
    FIRST_LAUNCH,

    /** Position connue et compte jouable : carte de poursuite + overlay inventaire. */
    GAME,
}

/**
 * Décide l'écran de démarrage à partir du **premier fix GPS** et de l'**état du compte**.
 *
 * Le premier fix **prime** : tant que [premierFix] est `null`, on reste sur [StartupStage.LOADING]
 * quel que soit l'état du compte — on ne montre jamais la carte (donc jamais un centre arbitraire) ni
 * la pose de base avant la position. Une fois la position connue, la pose de base
 * ([StartupStage.FIRST_LAUNCH]) s'affiche tant que le compte est prêt **sans** base ; sinon (base
 * posée, ou amorçage en cours/échoué) c'est la carte de jeu ([StartupStage.GAME], dont l'overlay
 * inventaire gère lui-même l'amorçage).
 *
 * La gestion de la **permission de localisation** n'entre pas ici : elle est assurée en amont par
 * [com.hexa.map.LocationPermissionGate], qui n'expose ce flux qu'une fois la permission accordée.
 *
 * @param premierFix dernière position filtrée connue, ou `null` tant qu'aucun fix n'est arrivé.
 * @param playerState état observable du compte joueur.
 */
fun startupStage(premierFix: LatLng?, playerState: PlayerUiState): StartupStage = when {
    premierFix == null -> StartupStage.LOADING
    playerState is PlayerUiState.Ready && playerState.baseCell == null -> StartupStage.FIRST_LAUNCH
    else -> StartupStage.GAME
}
