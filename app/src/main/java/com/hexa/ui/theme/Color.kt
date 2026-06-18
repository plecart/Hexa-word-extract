package com.hexa.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Direction artistique « carte sci-fi sombre » : fond anthracite, surfaces froides, accent cyan
// signal. Mode sombre **unique** — aucune variante claire. Valeurs calées à l'œil sur device.
private val Anthracite = Color(0xFF0B0E13)
private val SurfaceCool = Color(0xFF141A22)
private val SurfaceElevated = Color(0xFF1B232D)
private val Ink = Color(0xFFE7EDF4)
private val InkMuted = Color(0xFFA6B4C2)
private val CyanSignal = Color(0xFF4FD6E0)
private val DangerSignal = Color(0xFFFF5C63)
private val OutlineFaint = Color(0xFF2C3947)

/**
 * Tokens de surface réutilisables qui n'ont pas de slot dans [androidx.compose.material3.ColorScheme] :
 * translucidité des panneaux posés sur la carte et bordure fine lumineuse. Halos colorés : dérivés
 * d'une couleur d'élément à faible alpha au point d'usage (cf. [HexaElementColors]).
 */
object HexaSurfaceTokens {
    /** Fond de panneau semi-translucide (~80 %), laissant deviner la carte dessous. */
    val translucentSurface = Color(0xCC141A22)

    /** Bordure fine « lumineuse » : blanc glacé à faible alpha, indépendante de l'élément. */
    val border = Color(0x33BEDDFF)
}

/**
 * Couleurs d'identité des cinq éléments, **par rareté croissante** (cf. [com.hexa.config.Element]) :
 * Cendrite orange braise, Givrelin cyan givre, Lithosève vert sève, Échofer magenta résonant,
 * Nyctite violet nuit.
 *
 * Cette tranche n'expose que les **tokens** ; le mapping `Element → couleur` (icônes, tuiles) arrive
 * en #44. [all] suit l'ordre de `Element.entries`, pour un appariement positionnel direct.
 */
object HexaElementColors {
    val cendrite = Color(0xFFFF6A33)
    val givrelin = Color(0xFF49D6E6)
    val lithoseve = Color(0xFF5FCB6A)
    val echofer = Color(0xFFE94CC0)
    val nyctite = Color(0xFF8C6BFF)

    val all = listOf(cendrite, givrelin, lithoseve, echofer, nyctite)
}

/**
 * Schéma de couleurs sombre **unique** de l'application, branché par [HexaTheme]. Pas de variante
 * claire : la DA est exclusivement sombre. Les slots non renseignés gardent les valeurs sombres
 * Material 3 par défaut.
 */
val HexaDarkColorScheme =
    darkColorScheme(
        primary = CyanSignal,
        onPrimary = Anthracite,
        background = Anthracite,
        onBackground = Ink,
        surface = SurfaceCool,
        onSurface = Ink,
        surfaceVariant = SurfaceElevated,
        onSurfaceVariant = InkMuted,
        outline = OutlineFaint,
        error = DangerSignal,
        onError = Anthracite,
    )
