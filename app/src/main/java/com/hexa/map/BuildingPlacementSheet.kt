package com.hexa.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.inventory.labelOf
import com.hexa.player.BuildingType
import com.hexa.ui.theme.BuildingObject
import com.hexa.ui.theme.HexaActionButton
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.ObjectAssets
import com.hexa.ui.theme.hexaGlowSurface

/** Côté du marqueur « + », assez large pour rester une cible tactile confortable au-dessus de la tuile. */
private val MARKER_SIZE = 48.dp

/**
 * Marqueur **« + »** posé au-dessus de la tuile courante quand une pose est possible (cf.
 * [extractorPlacementCell]). Un tap ouvre la liste des bâtiments à poser ([BuildingPlacementSheet]).
 *
 * Pastille ronde à la DA « glow » cyan ; tout le disque est cliquable et porte la description d'accès,
 * pour être piloté au tap aussi bien qu'aux tests/à l'accessibilité.
 *
 * @param onClick ouverture de la liste de pose.
 */
@Composable
fun ExtractorPlacementMarker(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.placement_marker_description)
    Box(
        modifier
            .size(MARKER_SIZE)
            .hexaGlowSurface(shape = CircleShape, glow = MaterialTheme.colorScheme.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
    }
}

/**
 * Bottom sheet **liste des bâtiments à poser**, ouvert depuis le marqueur « + ». Au MVP, un seul type
 * — l'extracteur — avec son stock et un bouton « Poser ». Coquille `ModalBottomSheet` ; le corps est
 * extrait en [BuildingPlacementContent] pour les aperçus Studio et les tests UI Compose.
 *
 * @param extractorStock extracteurs construits prêts à poser (cf. [com.hexa.player.PlayerViewModel.extractorStock]).
 * @param onPlaceExtracteur pose effective de l'extracteur sur la tuile courante.
 * @param onDismiss fermeture du panneau (swipe ou tap hors panneau).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingPlacementSheet(
    extractorStock: Int,
    onPlaceExtracteur: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        BuildingPlacementContent(extractorStock = extractorStock, onPlaceExtracteur = onPlaceExtracteur)
    }
}

/**
 * Corps du panneau de pose, **indépendant du `ModalBottomSheet`** : titre puis une tuile par bâtiment
 * disponible. Extrait pour être rendu directement par les aperçus et les tests UI Compose.
 *
 * @param extractorStock nombre d'extracteurs prêts à poser, affiché et borné par le ViewModel amont.
 * @param onPlaceExtracteur déclenche la pose de l'extracteur.
 */
@Composable
internal fun BuildingPlacementContent(
    extractorStock: Int,
    onPlaceExtracteur: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.placement_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        BuildingPlacementRow(building = BuildingType.EXTRACTEUR, stock = extractorStock, onPlace = onPlaceExtracteur)
    }
}

/**
 * Tuile d'un bâtiment posable : icône + nom et stock à gauche, bouton « Poser » à droite. Liseré pris
 * dans la couleur d'identité du bâtiment, cohérent avec les tuiles d'inventaire et d'inspection.
 */
@Composable
private fun BuildingPlacementRow(building: BuildingType, stock: Int, onPlace: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hexaGlowSurface(shape = MaterialTheme.shapes.small, glow = ObjectAssets.of(building).color)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuildingObject(building, Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(labelOf(building)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.placement_stock, stock),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HexaActionButton(text = stringResource(R.string.placement_place_button), onClick = onPlace)
    }
}

/** Aperçu Studio de la liste de pose : l'extracteur avec un stock prêt à poser. */
@Preview(name = "Pose — bâtiments disponibles", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun BuildingPlacementPreview() {
    HexaTheme {
        BuildingPlacementContent(extractorStock = 3, onPlaceExtracteur = {})
    }
}
