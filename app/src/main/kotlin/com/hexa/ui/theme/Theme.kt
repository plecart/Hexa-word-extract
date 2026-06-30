package com.hexa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Thème Compose de l'application : direction artistique « carte sci-fi sombre ».
 *
 * Mode **sombre unique** — le réglage clair/sombre du système est volontairement ignoré (pas de
 * `isSystemInDarkTheme()`) : l'app n'a qu'une seule ambiance soignée. Branche la palette
 * ([HexaDarkColorScheme]), la typographie ([HexaTypography]) et les formes ([HexaShapes]) pour tous
 * les écrans qui s'enveloppent dedans.
 */
@Composable
fun HexaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HexaDarkColorScheme,
        typography = HexaTypography,
        shapes = HexaShapes,
        content = content,
    )
}
