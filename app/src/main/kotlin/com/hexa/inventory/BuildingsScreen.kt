package com.hexa.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.config.Element
import com.hexa.config.GameConfig
import com.hexa.player.BuildingType
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState
import com.hexa.ui.theme.BuildingObject
import com.hexa.ui.theme.ElementObject
import com.hexa.ui.theme.HexaActionButton
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.ObjectAssets
import com.hexa.ui.theme.hexaGlowSurface

/**
 * Page **Bâtiments** plein écran, ouverte par-dessus la carte depuis la barre flottante, habillée par
 * la DA « carte sci-fi sombre » via [OverlayScaffold] (fond anthracite plein, top bar translucide +
 * bouton fermer). Elle montre le **stock d'extracteurs** prêts à poser et la **recette de craft**.
 *
 * Le bouton « Construire » est **toujours actif** : la suffisance des ressources est tranchée par le
 * use case (la couture de validation, cf. #137), pas par l'UI ; un refus remonte via [craftShortfall]
 * et la carte affiche alors le détail des éléments manquants (cf. [CraftCard]).
 *
 * Pendant l'amorçage du compte ou après un échec, un panneau d'état centré remplace le contenu ; en
 * état prêt, les compteurs se mettent à jour sans action de l'utilisateur (le ViewModel observe le
 * document joueur en continu).
 *
 * @param state état du compte joueur (chargement, prêt, échec).
 * @param onClose ferme la page et redonne la carte (état de carte préservé : elle reste composée).
 * @param onCraftExtracteur déclenche le craft d'un extracteur (débit inventaire + crédit stock).
 * @param craftShortfall détail du dernier craft refusé (élément → unités manquantes), ou `null` si le
 *   dernier craft a réussi / aucun craft tenté (cf. [com.hexa.player.PlayerViewModel.craftShortfall]).
 */
@Composable
fun BuildingsScreen(
    state: PlayerUiState,
    onClose: () -> Unit,
    onCraftExtracteur: () -> Unit,
    modifier: Modifier = Modifier,
    craftShortfall: Map<Element, Long>? = null,
) {
    OverlayScaffold(
        title = stringResource(R.string.buildings_title),
        closeContentDescription = stringResource(R.string.buildings_close),
        onClose = onClose,
        modifier = modifier,
    ) { contentModifier ->
        OverlayStateContent(state, contentModifier) { ready, readyModifier ->
            ExtracteurSection(
                inventory = ready.inventory,
                stock = ready.builtBuildings[BuildingType.EXTRACTEUR] ?: 0,
                onCraft = onCraftExtracteur,
                shortfall = craftShortfall,
                modifier = readyModifier,
            )
        }
    }
}

/** Tuile du stock d'extracteurs prêts à poser, surmontant la carte de craft. */
@Composable
private fun ExtracteurSection(
    inventory: Inventory,
    stock: Int,
    onCraft: () -> Unit,
    shortfall: Map<Element, Long>?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BuildingStockRow(building = BuildingType.EXTRACTEUR, stock = stock)
        CraftCard(inventory = inventory, onCraft = onCraft, shortfall = shortfall)
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
 * ([GameConfig.RECIPE_EXTRACTEUR], ordonnée par rareté), le détail motivé d'un éventuel refus
 * ([shortfall]), puis le bouton « Construire ».
 *
 * Le bouton reste **toujours actif** : c'est le use case qui tranche la suffisance (la couture de
 * validation, cf. #137). Quand le dernier craft a été refusé, [shortfall] porte les unités manquantes
 * par élément et la carte les affiche en couleur d'erreur ; il vaut `null` après un succès.
 */
@Composable
private fun CraftCard(inventory: Inventory, onCraft: () -> Unit, shortfall: Map<Element, Long>?) {
    val recipe = GameConfig.RECIPE_EXTRACTEUR
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
        if (shortfall != null) CraftShortfall(shortfall)
        HexaActionButton(
            text = stringResource(R.string.inventory_craft_button),
            onClick = onCraft,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/**
 * Détail motivé d'un craft refusé : un en-tête « ressources insuffisantes » puis, par élément en
 * déficit (ordonné par rareté), le nombre d'unités manquantes — le tout en couleur d'erreur.
 *
 * @param shortfall élément → unités manquantes (`requis - possédé`, toujours > 0), non vide.
 */
@Composable
private fun CraftShortfall(shortfall: Map<Element, Long>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.inventory_craft_insufficient),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Element.entries.filter { it in shortfall }.forEach { element ->
            Text(
                stringResource(
                    R.string.inventory_craft_missing_item,
                    shortfall.getValue(element),
                    stringResource(labelOf(element)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
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

/**
 * Aperçu Studio de la page Bâtiments dans ses deux états de craft : ressources **suffisantes** (recette
 * couverte, aucun refus) et ressources **insuffisantes** (ligne Givrelin en rouge + détail motivé des
 * manquants). Le bouton reste actif dans les deux cas. Garde-fou visuel : une régression de la
 * lisibilité possédé/requis ou du message de refus saute aux yeux.
 */
@Preview(name = "Page Bâtiments — craft", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun BuildingsCraftPreview() {
    HexaTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ExtracteurSection(
                inventory = Inventory.of(GameConfig.STARTER_KIT),
                stock = 2,
                onCraft = {},
                shortfall = null,
            )
            ExtracteurSection(
                inventory = Inventory.of(mapOf(Element.CENDRITE to 120, Element.GIVRELIN to 10)),
                stock = 0,
                onCraft = {},
                // Givrelin manquant : 40 requis - 10 possédé.
                shortfall = mapOf(Element.GIVRELIN to 30L),
            )
        }
    }
}
