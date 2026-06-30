package com.hexa

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Passe la fenêtre en **mode immersif « sticky »** : masque les barres système (barre de statut et
 * barre de navigation / gestes) pour offrir un rendu carte **plein écran**, tout en laissant
 * l'utilisateur les faire **réapparaître transitoirement par un swipe** depuis un bord — après quoi
 * elles se re-masquent automatiquement.
 *
 * Le comportement [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE] est ce qui
 * distingue l'immersif « sticky » (barres transitoires, geste/bouton Retour préservés) de l'immersif
 * strict (barres définitivement masquées). Il doit être posé **avant** [WindowInsetsControllerCompat.hide]
 * pour que le premier masquage respecte déjà ce mode.
 *
 * Idempotent : ré-appelable sans effet de bord (p. ex. à chaque retour au premier plan). Le masquage
 * effectif des barres est un effet système observable **sur appareil réel** uniquement.
 *
 * @receiver le contrôleur d'insets de la fenêtre de l'activité (cf.
 *   `WindowCompat.getInsetsController(window, decorView)`).
 */
internal fun WindowInsetsControllerCompat.enableImmersiveSystemBars() {
    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    hide(WindowInsetsCompat.Type.systemBars())
}
