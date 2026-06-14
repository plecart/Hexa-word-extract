package com.hexa.player

/**
 * Types de bâtiments du jeu, libellés par un identifiant **contractuel** (un futur serveur lira ces
 * noms dans le document joueur). Au MVP, seul l'extracteur est craftable ; la base offerte est
 * fonctionnellement un extracteur non déplaçable (cf. PRD #5).
 */
enum class BuildingType {
    EXTRACTEUR,
}
