package com.hexa.player

import java.time.Instant

/**
 * Type d'un bâtiment **placé** sur une tuile, libellé par un identifiant **contractuel** (un futur
 * serveur lira ce nom dans le document `buildings/{h3Index}`, cf. spec F5).
 *
 * Distinct de [BuildingType], qui dénombre le **stock craftable** prêt à poser dans le document
 * joueur : ici on décrit ce qui est **déjà posé sur la grille**. Au MVP, seul le tout premier
 * placement — la base offerte — existe ; l'extracteur placé rejoindra cette énumération avec son
 * propre flux de placement.
 */
enum class PlacedBuildingType {
    BASE,
}

/**
 * Un bâtiment **posé sur une tuile**, image fidèle d'un document `players/{uid}/buildings/{h3Index}`
 * (cf. spec F5/F9). L'index H3 de la tuile sert d'identifiant du document, ce qui garantit
 * gratuitement la règle « un bâtiment par tuile ».
 *
 * Modèle pur (aucune dépendance Firebase) ; la traduction vers/depuis Firestore vit côté `:app`.
 *
 * @property cell index H3 de la tuile portant le bâtiment (= ID du document).
 * @property type type du bâtiment placé.
 * @property placedAt instant de la pose.
 * @property lastCollectedAt borne de départ du prochain calcul de récolte (cf. PRD #5) ; égale à
 *   [placedAt] à la pose, puis avancée à chaque récolte.
 */
data class PlacedBuilding(
    val cell: String,
    val type: PlacedBuildingType,
    val placedAt: Instant,
    val lastCollectedAt: Instant,
) {
    companion object {
        /**
         * Construit la **base offerte** posée en [cell] à l'instant [at] : type [PlacedBuildingType.BASE],
         * récolte amorcée au moment de la pose (`lastCollectedAt = placedAt = at`).
         */
        fun base(cell: String, at: Instant): PlacedBuilding =
            PlacedBuilding(cell = cell, type = PlacedBuildingType.BASE, placedAt = at, lastCollectedAt = at)
    }
}
