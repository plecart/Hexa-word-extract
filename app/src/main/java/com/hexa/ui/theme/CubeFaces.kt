package com.hexa.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Facteur d'éclaircissement de la face supérieure (vers le blanc). */
private const val TOP_LIGHTEN = 0.35f

/** Facteur d'assombrissement de la face gauche (vers le noir). */
private const val LEFT_DARKEN = 0.18f

/** Facteur d'assombrissement, plus marqué, de la face droite (vers le noir). */
private const val RIGHT_DARKEN = 0.42f

/**
 * Les trois faces visibles d'un cube en projection isométrique, déjà ombrées.
 *
 * @property top face supérieure (la plus éclairée).
 * @property left face latérale gauche (légèrement assombrie).
 * @property right face latérale droite (la plus assombrie).
 */
data class CubeFaces(val top: Color, val left: Color, val right: Color)

/**
 * Dérive les trois faces ombrées d'un cube placeholder à partir de sa seule couleur d'identité, en
 * mélangeant vers le blanc (dessus) ou le noir (côtés). C'est cet écart de luminosité — et non un
 * trait de contour — qui fait lire le carré comme un **bloc en relief**.
 *
 * Pur et déterministe : aucune dépendance à l'état Compose, donc testable isolément ([CubeFacesTest])
 * et réutilisable par n'importe quel rendu de placeholder.
 *
 * @param base couleur d'identité de l'élément (cf. [ElementVisuals]).
 * @return les trois faces, du plus clair (dessus) au plus sombre (droite).
 */
fun cubeFacesOf(base: Color): CubeFaces = CubeFaces(
    top = lerp(base, Color.White, TOP_LIGHTEN),
    left = lerp(base, Color.Black, LEFT_DARKEN),
    right = lerp(base, Color.Black, RIGHT_DARKEN),
)
