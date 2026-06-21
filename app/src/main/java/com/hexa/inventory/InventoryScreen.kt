package com.hexa.inventory

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.config.Element
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState
import com.hexa.ui.theme.AnimatedCount
import com.hexa.ui.theme.ElementObject
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.ObjectAssets
import com.hexa.ui.theme.hexaGlowSurface

/** Onglets de l'inventaire ; l'ordre des entrées fixe l'ordre d'affichage. */
private enum class InventoryTab(
    @StringRes val titleRes: Int,
) {
    RESOURCES(R.string.inventory_tab_resources),
    BUILDINGS(R.string.inventory_tab_buildings),
}

/**
 * Page d'inventaire à deux onglets, ouverte par-dessus la carte, habillée par la DA « carte sci-fi
 * sombre » : fond anthracite plein, barre et onglets translucides, ressources en tuiles dont le liseré
 * prend la couleur de l'élément (cf. [hexaGlowSurface], [ObjectAssets]).
 *
 * L'onglet **Ressources** liste les cinq éléments (par rareté croissante) avec leur quantité courante,
 * lue depuis [state] : comme le ViewModel observe le document joueur en continu, les compteurs se
 * mettent à jour sans action de l'utilisateur. L'onglet **Bâtiments** affiche un état vide (son
 * contenu arrive en #23).
 *
 * @param state état du compte joueur (chargement, prêt, échec).
 * @param onClose ferme la page et redonne la carte (état de carte préservé : elle reste composée).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(state: PlayerUiState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(InventoryTab.RESOURCES) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.inventory_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.inventory_close)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab.ordinal, containerColor = Color.Transparent) {
                InventoryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
            when (selectedTab) {
                InventoryTab.RESOURCES -> ResourcesTab(state, Modifier.fillMaxSize())
                InventoryTab.BUILDINGS -> CenteredPanel(
                    stringResource(R.string.inventory_buildings_empty),
                    Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Contenu de l'onglet Ressources selon l'état : panneau centré pendant/à l'échec, liste sinon. */
@Composable
private fun ResourcesTab(state: PlayerUiState, modifier: Modifier = Modifier) {
    when (state) {
        PlayerUiState.Loading -> CenteredPanel(stringResource(R.string.inventory_loading), modifier)
        PlayerUiState.Failed -> CenteredPanel(stringResource(R.string.inventory_error), modifier)
        is PlayerUiState.Ready -> ResourceList(state.inventory, modifier)
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
 * Tuile d'un élément : icône + nom à gauche, quantité à droite, le tout en panneau DA dont le
 * **liseré prend la couleur de l'élément** ([ObjectAssets]) — l'identité saute aux yeux sans lire le
 * nom. Le nom reste en corps système ; la quantité ressort en accent cyan, police d'affichage à
 * chiffres tabulaires (slot compteur, convention posée par la DA).
 *
 * @param element l'élément affiché (fixe la couleur, l'icône et le libellé).
 * @param amount quantité courante en stock.
 */
@Composable
private fun ResourceRow(element: Element, amount: Long) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .hexaGlowSurface(shape = MaterialTheme.shapes.small, glow = ObjectAssets.of(element).color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElementObject(element, Modifier.size(28.dp))
            Text(
                stringResource(labelOf(element)),
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

/** Message d'état (chargement, échec, onglet vide) centré dans un panneau DA discret. */
@Composable
private fun CenteredPanel(text: String, modifier: Modifier = Modifier) {
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
