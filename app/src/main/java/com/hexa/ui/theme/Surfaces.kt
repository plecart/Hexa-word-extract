package com.hexa.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Épaisseur de la bordure lumineuse des panneaux DA. */
private val BORDER_WIDTH = 1.dp

/**
 * Habille un composant en **panneau DA** : fond semi-translucide + bordure fine lumineuse, coins
 * nets. Brique réutilisable des surfaces posées sur la carte (lignes de ressource, panneaux d'état…).
 *
 * Aucune ombre portée Material (interdite par la DA) : la profondeur naît de la translucidité et de
 * la bordure, pas d'une élévation.
 *
 * @param shape forme des coins, à prendre dans `MaterialTheme.shapes` ([HexaShapes]).
 * @param glow couleur de la bordure lumineuse ; par défaut la bordure glacée neutre
 *   ([HexaSurfaceTokens.border]). Passer une couleur d'élément ([HexaElementColors]) donne un halo
 *   coloré (consommé par la tranche identité des éléments, #44).
 */
fun Modifier.hexaGlowSurface(shape: Shape, glow: Color = HexaSurfaceTokens.border): Modifier = this
    .background(color = HexaSurfaceTokens.translucentSurface, shape = shape)
    .border(BorderStroke(BORDER_WIDTH, glow), shape = shape)
