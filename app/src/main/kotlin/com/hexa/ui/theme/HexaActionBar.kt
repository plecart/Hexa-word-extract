package com.hexa.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Marge interne de l'icône au sein de sa cible tactile (centre l'icône, ~24 dp visibles). */
private val ICON_PADDING = 12.dp

/** Espacement entre items de la barre. */
private val ITEM_SPACING = 8.dp

/**
 * Opacité du **fond tonal** des pastilles (idiome Material 3 « filled-tonal ») : une teinte d'accent
 * très diluée qui signale le bouton par un fond — pas par une bordure (qui doublerait le cadre du
 * dock et évoquerait à tort un palier de rareté en grammaire d'UI de jeu). Le liseré lumineux reste
 * réservé au focus / à l'appui (ripple).
 */
private const val CHIP_FILL_ALPHA = 0.14f

/**
 * Une action de jeu présentée dans la [HexaActionBar] : un glyphe vectoriel cliquable, sans texte.
 *
 * @param icon glyphe vectoriel (p. ex. [Icons].Outlined.Diamond), teinté en accent par la barre.
 * @param contentDescription libellé d'accessibilité déjà résolu (chaîne, lu par TalkBack) — décrit
 *   l'action, jamais l'icône (« Ouvrir les ressources », pas « diamant »).
 * @param onClick invoqué à chaque tap sur l'item.
 */
data class HexaAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

/**
 * Barre d'actions de la carte, habillée selon la DA « carte sci-fi sombre » : un **dock** unique
 * [hexaGlowSurface] (surface translucide + liseré fin lumineux) hébergeant les [actions] côte à côte,
 * chacune en **pastille tonale** (icône en accent cyan sur fond d'accent dilué, sans second cadre).
 * Conçue pour **croître par extension** : ajouter une action de jeu = ajouter un [HexaAction] à la
 * liste, sans toucher au composant.
 *
 * Ancrée **en bas, centrée** : la barre épouse la largeur de son contenu et applique elle-même
 * [safeGesturesPadding] pour rester **hors de la zone de geste système**. En mode immersif (barres
 * masquées), l'inset de barre de navigation retombe à zéro : seul l'inset de **geste** garde la barre
 * à l'écart du swipe (bord du bas en navigation gestuelle, barre 3 boutons via `tappableElement`).
 * Le **placement** (alignement bas, centrage) est laissé à l'appelant via [modifier] — typiquement
 * `Modifier.align(Alignment.BottomCenter)`.
 *
 * @param actions actions à présenter, dans l'ordre d'affichage (gauche → droite).
 * @param modifier agencement décidé par l'appelant (alignement, marges) ; appliqué avant les insets
 *   et l'habillage, de sorte que la surface lumineuse épouse la taille de la barre.
 */
@Composable
fun HexaActionBar(actions: List<HexaAction>, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
        modifier
            .safeGesturesPadding()
            .clip(shape)
            .hexaGlowSurface(shape = shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action -> HexaActionBarItem(action) }
    }
}

/**
 * Un item de la barre : **pastille tonale** (fond d'accent dilué, sans bordure — idiome Material 3
 * « filled-tonal ») portant une icône en accent cyan, dans une cible tactile de
 * [HexaDimens.minTouchTarget]. L'appui est signalé par le ripple ; aucun second cadre, pour ne pas
 * doubler le liseré du dock.
 */
@Composable
private fun HexaActionBarItem(action: HexaAction) {
    val shape = MaterialTheme.shapes.medium
    val accent = MaterialTheme.colorScheme.primary
    Icon(
        imageVector = action.icon,
        contentDescription = action.contentDescription,
        tint = accent,
        modifier =
        Modifier
            .clip(shape)
            .background(color = accent.copy(alpha = CHIP_FILL_ALPHA), shape = shape)
            .clickable(role = Role.Button, onClick = action.onClick)
            .size(HexaDimens.minTouchTarget)
            .padding(ICON_PADDING),
    )
}

/** Aperçu Studio : la barre avec son unique action (ressources), sur fond DA. */
@Preview(name = "Barre d'actions", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun HexaActionBarPreview() {
    HexaTheme {
        HexaActionBar(
            actions =
            listOf(
                HexaAction(
                    icon = Icons.Outlined.Diamond,
                    contentDescription = "Ouvrir les ressources",
                    onClick = {},
                ),
            ),
        )
    }
}
