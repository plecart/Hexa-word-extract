package com.hexa.inventory

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.config.Element
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState

/** Onglets de l'inventaire ; l'ordre des entrées fixe l'ordre d'affichage. */
private enum class InventoryTab(
    @StringRes val titleRes: Int,
) {
    RESOURCES(R.string.inventory_tab_resources),
    BUILDINGS(R.string.inventory_tab_buildings),
}

/**
 * Page d'inventaire à deux onglets, ouverte par-dessus la carte.
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory_title)) },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.inventory_close)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                InventoryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.titleRes)) },
                    )
                }
            }
            when (selectedTab) {
                InventoryTab.RESOURCES -> ResourcesTab(state, Modifier.fillMaxSize())
                InventoryTab.BUILDINGS -> CenteredMessage(
                    stringResource(R.string.inventory_buildings_empty),
                    Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Contenu de l'onglet Ressources selon l'état : message centré pendant/à l'échec, liste sinon. */
@Composable
private fun ResourcesTab(state: PlayerUiState, modifier: Modifier = Modifier) {
    when (state) {
        PlayerUiState.Loading -> CenteredMessage(stringResource(R.string.inventory_loading), modifier)
        PlayerUiState.Failed -> CenteredMessage(stringResource(R.string.inventory_error), modifier)
        is PlayerUiState.Ready -> ResourceList(state.inventory, modifier)
    }
}

/** Les cinq compteurs, dans l'ordre de rareté de [Element]. */
@Composable
private fun ResourceList(inventory: Inventory, modifier: Modifier = Modifier) {
    LazyColumn(modifier) {
        items(Element.entries) { element ->
            ResourceRow(name = stringResource(labelOf(element)), amount = inventory[element])
        }
    }
}

/** Une ligne « nom de l'élément … quantité ». */
@Composable
private fun ResourceRow(name: String, amount: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        Text(amount.toString(), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Message unique centré (chargement, échec, état vide). */
@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
