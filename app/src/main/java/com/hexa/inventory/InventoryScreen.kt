package com.hexa.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.config.Element
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState
import com.hexa.ui.theme.ElementObject
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.ObjectAssets
import com.hexa.ui.theme.hexaGlowSurface

/**
 * Page d'inventaire (sac à dos) plein écran, ouverte par-dessus la carte, habillée par la DA « carte
 * sci-fi sombre » via [OverlayScaffold] (fond anthracite plein, top bar translucide + bouton fermer),
 * ses ressources en tuiles dont le liseré prend la couleur de l'élément (cf. [hexaGlowSurface],
 * [ObjectAssets]).
 *
 * Elle liste les cinq éléments (par rareté croissante) avec leur quantité courante, lue depuis [state] :
 * comme le ViewModel observe le document joueur en continu, les compteurs se mettent à jour sans action
 * de l'utilisateur. Le stock de bâtiments et le craft vivent désormais sur leur propre page
 * ([BuildingsScreen]), ouverte par une action dédiée de la barre flottante.
 *
 * @param state état du compte joueur (chargement, prêt, échec).
 * @param onClose ferme la page et redonne la carte (état de carte préservé : elle reste composée).
 */
@Composable
fun InventoryScreen(state: PlayerUiState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    OverlayScaffold(
        title = stringResource(R.string.inventory_title),
        closeContentDescription = stringResource(R.string.inventory_close),
        onClose = onClose,
        modifier = modifier,
    ) { contentModifier ->
        OverlayStateContent(state, contentModifier) { ready, readyModifier ->
            ResourceList(ready.inventory, readyModifier)
        }
    }
}

/** Les cinq tuiles de ressource espacées, dans l'ordre de rareté de [Element]. */
@Composable
private fun ResourceList(inventory: Inventory, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(Element.entries) { element ->
            ResourceRow(element = element, amount = inventory[element])
        }
    }
}

/**
 * Tuile d'un élément : son icône à 28 dp, son libellé, et sa quantité courante — habillée en
 * [GlowTile] dont le liseré prend la couleur de l'élément ([ObjectAssets]).
 *
 * @param element l'élément affiché (fixe la couleur, l'icône et le libellé).
 * @param amount quantité courante en stock.
 */
@Composable
private fun ResourceRow(element: Element, amount: Long) {
    GlowTile(
        glow = ObjectAssets.of(element).color,
        label = stringResource(labelOf(element)),
        amount = amount,
    ) {
        ElementObject(element, Modifier.size(28.dp))
    }
}

/**
 * Aperçu Studio des cinq tuiles de ressource — une par élément, dans l'ordre de rareté, avec son
 * icône, son liseré coloré et une quantité d'exemple (dont un zéro et un grand nombre, pour les
 * chiffres tabulaires). Garde-fou visuel : une régression de l'identité d'un élément saute aux yeux.
 */
@Preview(name = "Tuiles de ressource", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun ResourceTilesPreview() {
    val sampleAmounts = listOf(1_240L, 87L, 5L, 0L, 999_999L)
    HexaTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Element.entries.forEachIndexed { index, element ->
                ResourceRow(element = element, amount = sampleAmounts[index % sampleAmounts.size])
            }
        }
    }
}
