package com.hexa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formes du thème : **coins nets** (4–8 dp), conformes à la DA « carte sci-fi ». Volontairement loin
 * des grands arrondis Material par défaut, pour une silhouette technique plutôt que douce.
 */
val HexaShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(8.dp),
    )
