package com.hexa.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.player.PlayerUiState
import com.hexa.ui.theme.AnimatedCount
import com.hexa.ui.theme.hexaGlowSurface

/**
 * Briques DA partagées par les pages plein écran logées au-dessus de la carte ([InventoryScreen],
 * [BuildingsScreen]) : la coquille d'écran à top bar + bouton fermer ([OverlayScaffold]), la tuile à
 * liseré coloré ([GlowTile]) et le panneau de message d'état centré ([CenteredPanel]). Extraites ici
 * pour rester l'unique source des deux écrans, sans qu'aucun ne dépende d'un `internal` logé dans
 * l'autre.
 */

/**
 * Coquille commune des pages plein écran ouvertes par-dessus la carte : fond anthracite plein
 * ([MaterialTheme.colorScheme.background]) et **top bar translucide** portant le [title] à gauche et un
 * unique bouton fermer (icône `Close`, décrit par [closeContentDescription]) à droite. Le [content]
 * reçoit un `Modifier` déjà inseté de la top bar et étiré plein écran, prêt à recevoir le corps de la
 * page. Factorise le shell identique de l'inventaire et des bâtiments (un seul endroit où l'habillage
 * DA de page évolue).
 *
 * @param title titre affiché en haut à gauche.
 * @param closeContentDescription libellé d'accessibilité du bouton fermer (décrit l'action, pas l'icône).
 * @param onClose invoqué au tap sur le bouton fermer.
 * @param content corps de la page ; reçoit le `Modifier` inseté + plein écran à appliquer à sa racine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OverlayScaffold(
    title: String,
    closeContentDescription: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = closeContentDescription)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        content(Modifier.padding(padding).fillMaxSize())
    }
}

/**
 * Porte d'état commune aux corps de page : pendant l'amorçage du compte ([PlayerUiState.Loading]) ou
 * après un échec ([PlayerUiState.Failed]), affiche le panneau de message centré idoine ; en état prêt,
 * délègue à [ready] le rendu propre à la page (liste de ressources, stock + craft…). Centralise le
 * traitement Loading/Failed pour que chaque page ne décrive que son contenu prêt.
 *
 * @param state état du compte joueur.
 * @param modifier appliqué au panneau d'état comme au contenu prêt.
 * @param ready rendu du contenu en état prêt ; reçoit l'état prêt et le `Modifier` à appliquer.
 */
@Composable
internal fun OverlayStateContent(
    state: PlayerUiState,
    modifier: Modifier = Modifier,
    ready: @Composable (PlayerUiState.Ready, Modifier) -> Unit,
) {
    when (state) {
        PlayerUiState.Loading -> CenteredPanel(stringResource(R.string.inventory_loading), modifier)
        PlayerUiState.Failed -> CenteredPanel(stringResource(R.string.inventory_error), modifier)
        is PlayerUiState.Ready -> ready(state, modifier)
    }
}

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
