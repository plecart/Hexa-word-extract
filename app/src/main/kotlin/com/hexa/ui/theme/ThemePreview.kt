package com.hexa.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Planche d'aperçu Studio de la direction artistique : pastilles de la palette d'éléments, panneau
 * habillé ([hexaGlowSurface]), typographie (titre Chakra Petch, compteur à chiffres tabulaires en
 * accent cyan) et bouton d'action commun ([HexaActionButton]). Sert de référence visuelle et de
 * garde-fou : une régression du thème saute aux yeux dans le volet `@Preview`.
 */
@Preview(name = "Thème Hexa", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun HexaThemePreview() {
    HexaTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Inventaire",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HexaElementColors.all.forEach { color ->
                    Spacer(Modifier.size(36.dp).background(color, MaterialTheme.shapes.small))
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .hexaGlowSurface(shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Cendrite",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "1234567",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HexaActionButton(text = "Poser ma base", onClick = {})
        }
    }
}
