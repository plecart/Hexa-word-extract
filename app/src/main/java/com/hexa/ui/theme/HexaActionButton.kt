package com.hexa.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Hauteur de cible tactile minimale (recommandation d'accessibilité Material). */
private val MIN_TOUCH_TARGET = 48.dp

/**
 * Bouton d'action habillé selon la DA « carte sci-fi sombre » : un panneau [hexaGlowSurface]
 * (surface translucide + bordure fine lumineuse, coins nets) portant un libellé en accent cyan.
 *
 * **Aucune élévation Material** : contrairement au FAB / `Button` Material par défaut, il ne projette
 * pas d'ombre portée (interdite par la DA) — la profondeur naît de la translucidité et de la bordure.
 * Il partage ainsi le langage visuel des panneaux de l'inventaire ([hexaGlowSurface]), pour une
 * cohérence cross-écran.
 *
 * Brique commune des actions hors inventaire : les FAB flottants posés sur la carte (recentrer la
 * caméra, ouvrir l'inventaire) comme les boutons inline (réessayer la permission). Le **placement**
 * — alignement, marges, flottant ou centré — est laissé à l'appelant via [modifier].
 *
 * @param text libellé de l'action, déjà résolu (chaîne, pas d'id de ressource).
 * @param onClick invoqué à chaque tap.
 * @param modifier agencement décidé par l'appelant (alignement, marges) ; appliqué avant l'habillage,
 *   de sorte que la surface lumineuse épouse la taille du bouton.
 */
@Composable
fun HexaActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier =
        modifier
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .hexaGlowSurface(shape = shape)
            .defaultMinSize(minHeight = MIN_TOUCH_TARGET)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}
