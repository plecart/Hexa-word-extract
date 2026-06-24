package com.hexa.firstlaunch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.HexaApplication
import com.hexa.R
import com.hexa.map.H3Grid
import com.hexa.player.FirebaseAuthGateway
import com.hexa.player.FirestoreBuildingsRepository
import com.hexa.player.FirestorePlayerRepository
import com.hexa.player.PlaceBaseUseCase
import com.hexa.ui.theme.HexaActionButton
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.hexaGlowSurface
import com.uber.h3core.H3Core
import java.time.Clock

/**
 * Écran de **premier lancement** : tant que le joueur n'a pas de base, il s'affiche **par-dessus la
 * carte** (composée dessous) et l'invite à poser sa base sur sa tuile courante (cf. PRD #5, US 3-5).
 *
 * Glu mince qui câble le [FirstLaunchViewModel] sur les implémentations Firebase et n'expose au
 * panneau que ce dont il a besoin : peut-on poser (tuile connue et pose non en cours) et attend-on
 * encore la position. La pose effective, l'écriture de `baseCell` et du document bâtiment vivent dans
 * [PlaceBaseUseCase]. Une fois la base posée, le document joueur observé bascule [PlayerViewModel]
 * vers la carte de jeu : cet écran disparaît de lui-même.
 */
@Composable
fun FirstLaunchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HexaApplication
    val viewModel: FirstLaunchViewModel = viewModel(factory = firstLaunchViewModelFactory(app))
    val currentTile by viewModel.currentTile.collectAsStateWithLifecycle()
    val placing by viewModel.placing.collectAsStateWithLifecycle()

    FirstLaunchPanel(
        canPlace = currentTile != null && !placing,
        awaitingPosition = currentTile == null,
        onPlaceBase = viewModel::placeBase,
        modifier = modifier,
    )
}

/**
 * Panneau d'invitation, sans état : un encart sombre en bas de l'écran (la carte reste visible
 * au-dessus) portant le message et le bouton « Poser ma base ici ». Le bouton n'est actif que
 * lorsque [canPlace] ; tant que [awaitingPosition], un indice explique l'attente du GPS.
 */
@Composable
internal fun FirstLaunchPanel(
    canPlace: Boolean,
    awaitingPosition: Boolean,
    onPlaceBase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
            Modifier
                .padding(24.dp)
                .hexaGlowSurface(shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.first_launch_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.first_launch_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            HexaActionButton(
                text = stringResource(R.string.first_launch_place_base),
                onClick = onPlaceBase,
                enabled = canPlace,
            )
            if (awaitingPosition) {
                Text(
                    text = stringResource(R.string.first_launch_awaiting_position),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Câble le [FirstLaunchViewModel] : position GPS partagée ([HexaApplication.sharedPositionSource]),
 * grille H3 native, et pose adossée à Firestore (joueur + sous-collection bâtiments) via
 * [PlaceBaseUseCase].
 */
private fun firstLaunchViewModelFactory(app: HexaApplication) = viewModelFactory {
    initializer {
        val players = FirestorePlayerRepository()
        FirstLaunchViewModel(
            positionSource = app.sharedPositionSource,
            grid = H3Grid(h3 = H3Core.newSystemInstance()),
            placeBaseAt =
            PlaceBaseUseCase(
                auth = FirebaseAuthGateway(),
                players = players,
                buildings = FirestoreBuildingsRepository(),
                clock = Clock.systemUTC(),
            )::invoke,
        )
    }
}

@Preview
@Composable
private fun FirstLaunchPanelReadyPreview() {
    HexaTheme {
        FirstLaunchPanel(canPlace = true, awaitingPosition = false, onPlaceBase = {})
    }
}

@Preview
@Composable
private fun FirstLaunchPanelAwaitingPreview() {
    HexaTheme {
        FirstLaunchPanel(canPlace = false, awaitingPosition = true, onPlaceBase = {})
    }
}
