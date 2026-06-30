package com.hexa.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Tokens de dimension réutilisables du design-system, sans slot dédié dans le thème Material.
 * Source unique des valeurs partagées entre composants, pour éviter de redéfinir une même mesure
 * d'un fichier à l'autre.
 */
object HexaDimens {
    /**
     * Cible tactile minimale (48×48 dp), recommandation d'accessibilité Material : tout élément
     * interactif (boutons, items de barre d'actions) doit l'atteindre pour rester confortable au
     * pouce et navigable par TalkBack.
     */
    val minTouchTarget = 48.dp
}
