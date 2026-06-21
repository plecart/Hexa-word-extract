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
import com.hexa.config.GameConfig
import com.hexa.player.BuildingType
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState
import com.hexa.ui.theme.AnimatedCount
import com.hexa.ui.theme.BuildingObject
import com.hexa.ui.theme.ElementObject
import com.hexa.ui.theme.HexaActionButton
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
 * mettent à jour sans action de l'utilisateur. L'onglet **Bâtiments** montre le stock d'extracteurs
 * prêts à poser et la recette de craft, avec un bouton « Construire » (cf. [BuildingsTab]).
 *
 * @param state état du compte joueur (chargement, prêt, échec).
 * @param onClose ferme la page et redonne la carte (état de carte préservé : elle reste composée).
 * @param onCraftExtracteur déclenche le craft d'un extracteur (débit inventaire + crédit stock).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    state: PlayerUiState,
    onClose: () -> Unit,
    onCraftExtracteur: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                InventoryTab.BUILDINGS -> BuildingsTab(state, onCraftExtracteur, Modifier.fillMaxSize())
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
 * Tuile DA générique : une [icon] + un [label] à gauche, un compteur animé ([amount]) à droite, le
 * tout en panneau dont le **liseré prend la couleur [glow]** — l'identité saute aux yeux sans lire le
 * nom. Brique commune des tuiles de ressource ([ResourceRow]) et de bâtiment ([BuildingStockRow]) :
 * le nom reste en corps système, la quantité ressort en accent cyan à chiffres tabulaires (slot
 * compteur posé par la DA).
 *
 * @param glow couleur d'identité du liseré.
 * @param label nom affiché à gauche de l'icône.
 * @param amount quantité affichée à droite (défile à chaque variation).
 * @param icon contenu de l'icône (taille fixée par l'appelant), à gauche du nom.
 */
@Composable
private fun GlowTile(glow: Color, label: String, amount: Long, icon: @Composable () -> Unit) {
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

/** Contenu de l'onglet Bâtiments selon l'état : panneau centré pendant/à l'échec, le craft sinon. */
@Composable
private fun BuildingsTab(state: PlayerUiState, onCraftExtracteur: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        PlayerUiState.Loading -> CenteredPanel(stringResource(R.string.inventory_loading), modifier)
        PlayerUiState.Failed -> CenteredPanel(stringResource(R.string.inventory_error), modifier)
        is PlayerUiState.Ready -> ExtracteurSection(
            inventory = state.inventory,
            stock = state.builtBuildings[BuildingType.EXTRACTEUR] ?: 0,
            onCraft = onCraftExtracteur,
            modifier = modifier,
        )
    }
}

/** Tuile du stock d'extracteurs prêts à poser, surmontant la carte de craft. */
@Composable
private fun ExtracteurSection(inventory: Inventory, stock: Int, onCraft: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BuildingStockRow(building = BuildingType.EXTRACTEUR, stock = stock)
        CraftCard(inventory = inventory, onCraft = onCraft)
    }
}

/**
 * Tuile de stock d'un bâtiment : son icône à 28 dp, son libellé, et le nombre d'exemplaires prêts à
 * poser — même [GlowTile] que la tuile de ressource, liseré pris dans la couleur d'identité du bâtiment.
 *
 * @param building le bâtiment affiché (fixe couleur, icône et libellé).
 * @param stock nombre d'exemplaires construits prêts à poser.
 */
@Composable
private fun BuildingStockRow(building: BuildingType, stock: Int) {
    GlowTile(
        glow = ObjectAssets.of(building).color,
        label = stringResource(labelOf(building)),
        amount = stock.toLong(),
    ) {
        BuildingObject(building, Modifier.size(28.dp))
    }
}

/**
 * Carte de craft de l'extracteur : titre, une ligne possédé/requis par ressource de la recette
 * ([GameConfig.RECIPE_EXTRACTEUR], ordonnée par rareté), et le bouton « Construire » **désactivé** tant
 * qu'une ressource manque — un craft impossible ne peut donc pas être lancé (aucun débit).
 */
@Composable
private fun CraftCard(inventory: Inventory, onCraft: () -> Unit) {
    val recipe = GameConfig.RECIPE_EXTRACTEUR
    val affordable = recipe.all { (element, cost) -> inventory[element] >= cost }
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .hexaGlowSurface(shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.inventory_craft_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Element.entries.filter { it in recipe }.forEach { element ->
            RecipeRow(element = element, owned = inventory[element], required = recipe.getValue(element))
        }
        HexaActionButton(
            text = stringResource(R.string.inventory_craft_button),
            onClick = onCraft,
            enabled = affordable,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/**
 * Ligne de recette : icône + nom de la ressource à gauche, « possédé / requis » à droite — passe en
 * **couleur d'erreur** tant que le possédé ne couvre pas le requis, pour repérer le manque d'un coup d'œil.
 *
 * @param element ressource exigée par la recette.
 * @param owned quantité actuellement en stock.
 * @param required quantité exigée par la recette.
 */
@Composable
private fun RecipeRow(element: Element, owned: Long, required: Int) {
    val enough = owned >= required
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElementObject(element, Modifier.size(22.dp))
            Text(
                stringResource(labelOf(element)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            stringResource(R.string.inventory_craft_amount, owned, required),
            style = MaterialTheme.typography.titleSmall,
            color = if (enough) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
    }
}

/** Message d'état (chargement, échec) centré dans un panneau DA discret. */
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

/**
 * Aperçu Studio de l'onglet Bâtiments dans ses deux états de craft : ressources **suffisantes**
 * (bouton actif) et ressources **insuffisantes** (ligne Givrelin en rouge, bouton désactivé). Garde-fou
 * visuel : une régression de la lisibilité possédé/requis ou de l'état du bouton saute aux yeux.
 */
@Preview(name = "Onglet Bâtiments — craft", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun BuildingsCraftPreview() {
    HexaTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ExtracteurSection(inventory = Inventory.of(GameConfig.STARTER_KIT), stock = 2, onCraft = {})
            ExtracteurSection(
                inventory = Inventory.of(mapOf(Element.CENDRITE to 120, Element.GIVRELIN to 10)),
                stock = 0,
                onCraft = {},
            )
        }
    }
}
