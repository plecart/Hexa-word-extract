package com.hexa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hexa.R

/**
 * Police d'affichage **Chakra Petch** (SIL Open Font License — cf. `licenses/ChakraPetch-OFL.txt`),
 * embarquée en deux graisses : Regular (compteurs) et SemiBold (titres). Le corps de texte reste sur
 * la sans-serif système (slots `body*` non surchargés).
 */
private val ChakraPetch =
    FontFamily(
        Font(R.font.chakra_petch_regular, FontWeight.Normal),
        Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    )

private val Base = Typography()

// Chiffres tabulaires : largeur de glyphe fixe, pour que les quantités ne « sautent » pas quand
// elles changent (cf. compteurs temps réel de l'inventaire).
private const val TABULAR_FIGURES = "tnum"

/**
 * Typographie de l'application : Chakra Petch SemiBold sur les titres et libellés, Chakra Petch
 * Regular à chiffres tabulaires sur le **slot compteur** ([Typography.titleMedium], réservé aux
 * quantités numériques). Tous les autres slots conservent la sans système Material 3.
 */
val HexaTypography =
    Base.copy(
        titleLarge = Base.titleLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        titleMedium =
        Base.titleMedium.copy(
            fontFamily = ChakraPetch,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        labelLarge = Base.labelLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
    )
