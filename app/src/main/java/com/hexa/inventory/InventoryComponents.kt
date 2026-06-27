package com.hexa.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hexa.ui.theme.AnimatedCount
import com.hexa.ui.theme.hexaGlowSurface

/**
 * Briques DA partagées par les pages logées au-dessus de la carte ([InventoryScreen],
 * [BuildingsScreen]) : la tuile à liseré coloré ([GlowTile]) et le panneau de message d'état centré
 * ([CenteredPanel]). Extraites ici pour rester l'unique source des deux écrans, sans qu'aucun ne
 * dépende d'un `internal` logé dans l'autre.
 */

/**
 * Tuile DA générique : une [icon] + un [label] à gauche, un compteur animé ([amount]) à droite, le
 * tout en panneau dont le **liseré prend la couleur [glow]** — l'identité saute aux yeux sans lire le
 * nom. Brique commune des tuiles de ressource et de bâtiment : le nom reste en corps système, la
 * quantité ressort en accent cyan à chiffres tabulaires (slot compteur posé par la DA).
 *
 * @param glow couleur d'identité du liseré.
 * @param label nom affiché à gauche de l'icône.
 * @param amount quantité affichée à droite (défile à chaque variation).
 * @param icon contenu de l'icône (taille fixée par l'appelant), à gauche du nom.
 */
@Composable
internal fun GlowTile(glow: Color, label: String, amount: Long, icon: @Composable () -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .hexaGlowSurface(shape = MaterialTheme.shapes.small, glow = glow)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        AnimatedCount(
            amount = amount,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Message d'état (chargement, échec) centré dans un panneau DA discret. */
@Composable
internal fun CenteredPanel(text: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            modifier =
            Modifier
                .hexaGlowSurface(shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
