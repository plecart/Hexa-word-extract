package com.hexa.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.config.Element
import com.hexa.inventory.labelOf
import com.hexa.player.PlacementDecision
import com.hexa.player.PlacementRefusal
import com.hexa.ui.theme.ElementObject
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.ObjectAssets
import com.hexa.ui.theme.hexaGlowSurface
import com.hexa.world.ElementDeposit
import kotlin.math.roundToInt

/**
 * Bottom sheet d'**inspection de tuile** : ouvert d'un tap sur la carte, il liste le contenu de la
 * tuile touchée — chaque gisement avec son élément, sa richesse et sa vitesse d'extraction — ou un
 * **état vide explicite** si la tuile ne contient rien. Un badge « vous êtes ici » signale la tuile
 * courante du joueur.
 *
 * Habillé par la DA « carte sci-fi sombre », cohérent avec les tuiles d'inventaire : chaque ligne est
 * un panneau dont le liseré prend la couleur d'identité de l'élément ([ObjectAssets]) et porte son
 * icône ([ElementObject]). Le contenu est fourni recalculé à la volée par [TileInspectionViewModel] :
 * ce composable ne fait que l'afficher.
 *
 * @param inspection l'état du panneau (contenu de la tuile, indicateur « tuile courante »).
 * @param onDismiss appelé quand l'utilisateur referme le panneau (swipe ou tap hors panneau).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileInspectionSheet(inspection: TileInspection, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        TileInspectionContent(inspection)
    }
}

/**
 * Corps du panneau d'inspection, **indépendant du `ModalBottomSheet`** : en-tête puis liste des
 * gisements ou état vide selon [TileInspection.isEmpty]. Extrait de [TileInspectionSheet] pour être
 * rendu directement — par les aperçus Studio et par les tests UI Compose (qui n'ont pas à composer la
 * coquille `ModalBottomSheet`, fenêtre séparée difficile à piloter hors instrumentation).
 *
 * @param inspection contenu de la tuile et indicateur « tuile courante ».
 */
@Composable
internal fun TileInspectionContent(inspection: TileInspection, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InspectionHeader(isCurrent = inspection.isCurrent)
        PlacementStatus(inspection.placement)
        if (inspection.isEmpty) {
            EmptyTile()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                inspection.deposits.forEach { DepositRow(it) }
            }
        }
    }
}

/**
 * Ligne de **statut de pose** : indique en clair si un extracteur peut être posé sur la tuile
 * inspectée, ou la raison du refus. Indépendante du contenu de la tuile (présente aussi sur une tuile
 * vide). Panneau distinct dont le liseré prend l'accent `primary` quand la pose est possible, la
 * bordure neutre sinon — l'habillage reste validé à l'œil, seul le texte est couvert par les tests.
 */
@Composable
private fun PlacementStatus(placement: PlacementDecision) {
    val placeable = placement == PlacementDecision.Placeable
    // Halo d'accent quand la pose est possible ; bordure glacée neutre par défaut sinon (défaut de
    // hexaGlowSurface, non recopié ici pour ne pas dupliquer la couleur de bordure).
    val panel = if (placeable) {
        Modifier.hexaGlowSurface(shape = MaterialTheme.shapes.small, glow = MaterialTheme.colorScheme.primary)
    } else {
        Modifier.hexaGlowSurface(shape = MaterialTheme.shapes.small)
    }
    Text(
        text = placementStatusLabel(placement),
        modifier = Modifier
            .fillMaxWidth()
            .then(panel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = if (placeable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Traduit une [PlacementDecision] en libellé affichable. Le `when` sur [PlacementRefusal] est
 * **exhaustif** (sans `else`) : c'est le consommateur qui donne un rôle à chaque raison de refus —
 * ajouter une raison au domaine casse la compilation ici tant qu'on ne l'a pas libellée.
 */
@Composable
private fun placementStatusLabel(placement: PlacementDecision): String = when (placement) {
    PlacementDecision.Placeable -> stringResource(R.string.placement_status_placeable)
    is PlacementDecision.Refused -> when (placement.reason) {
        PlacementRefusal.NOT_CURRENT_TILE -> stringResource(R.string.placement_status_not_current_tile)
        PlacementRefusal.TILE_OCCUPIED -> stringResource(R.string.placement_status_tile_occupied)
        PlacementRefusal.NO_STOCK -> stringResource(R.string.placement_status_no_stock)
    }
}

/** Titre du panneau, suivi du badge « vous êtes ici » quand la tuile inspectée est la tuile courante. */
@Composable
private fun InspectionHeader(isCurrent: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.tile_inspection_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isCurrent) {
            Text(
                stringResource(R.string.tile_inspection_here),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .hexaGlowSurface(shape = MaterialTheme.shapes.small, glow = MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * Ligne d'un gisement : icône + nom de l'élément et sa richesse à gauche, vitesse d'extraction en
 * accent à droite. Liseré coloré à l'identité de l'élément, comme les tuiles d'inventaire.
 */
@Composable
private fun DepositRow(deposit: ElementDeposit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hexaGlowSurface(shape = MaterialTheme.shapes.small, glow = ObjectAssets.of(deposit.element).color)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElementObject(deposit.element, Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(labelOf(deposit.element)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.tile_deposit_richness, (deposit.richness * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(R.string.tile_deposit_rate, deposit.ratePerHour),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** État vide : la tuile ne contient aucune ressource. */
@Composable
private fun EmptyTile() {
    Text(
        stringResource(R.string.tile_inspection_empty),
        modifier = Modifier
            .fillMaxWidth()
            .hexaGlowSurface(shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Aperçu Studio du panneau peuplé : la tuile courante avec trois gisements de raretés différentes et
 * une pose possible, pour vérifier d'un coup d'œil l'identité colorée, le badge, la ligne de statut de
 * pose et le format richesse/vitesse.
 */
@Preview(name = "Inspection — tuile peuplée", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun PopulatedTilePreview() {
    HexaTheme {
        TileInspectionContent(
            TileInspection(
                deposits = listOf(
                    ElementDeposit(Element.CENDRITE, richness = 0.82, ratePerHour = 52),
                    ElementDeposit(Element.LITHOSEVE, richness = 0.45, ratePerHour = 9),
                    ElementDeposit(Element.NYCTITE, richness = 0.13, ratePerHour = 1),
                ),
                isCurrent = true,
                placement = PlacementDecision.Placeable,
            ),
        )
    }
}

/** Aperçu Studio de l'état vide, sur une tuile distante (pose refusée : pas la tuile courante). */
@Preview(name = "Inspection — tuile vide", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun EmptyTilePreview() {
    HexaTheme {
        TileInspectionContent(
            TileInspection(
                deposits = emptyList(),
                isCurrent = false,
                placement = PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE),
            ),
        )
    }
}
