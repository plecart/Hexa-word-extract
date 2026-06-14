package com.hexa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.inventory.InventoryScreen
import com.hexa.map.MapScreen
import com.hexa.player.EnsurePlayerUseCase
import com.hexa.player.FirebaseAuthGateway
import com.hexa.player.FirestorePlayerRepository
import com.hexa.player.PlayerViewModel
import com.hexa.ui.theme.HexaTheme
import com.mapbox.common.MapboxOptions
import java.time.Clock

/**
 * Unique activité de l'application (single-activity). Fournit le token public au SDK Mapbox, pose le
 * thème Compose et affiche la carte plein écran avec, par-dessus, la page d'inventaire ouverte par un
 * bouton flottant. Au démarrage, l'amorçage silencieux du compte joueur ([PlayerViewModel]) alimente
 * l'inventaire en temps réel.
 */
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels { playerViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Le SDK Mapbox lit le token public ici, avant toute instanciation de carte.
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        enableEdgeToEdge()
        setContent {
            HexaTheme {
                MapWithInventory(playerViewModel)
            }
        }
    }
}

/**
 * Compose la carte et superpose l'inventaire. La carte **reste composée** sous l'inventaire : son
 * état (position, zoom de la caméra) est ainsi préservé pendant l'aller-retour. Le bouton retour
 * système ferme l'inventaire plutôt que de quitter l'app.
 */
@Composable
private fun MapWithInventory(viewModel: PlayerViewModel) {
    var inventoryOpen by rememberSaveable { mutableStateOf(false) }
    val playerState by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        MapScreen(modifier = Modifier.fillMaxSize())

        if (inventoryOpen) {
            InventoryScreen(
                state = playerState,
                onClose = { inventoryOpen = false },
                modifier = Modifier.fillMaxSize(),
            )
            BackHandler { inventoryOpen = false }
        } else {
            ExtendedFloatingActionButton(
                onClick = { inventoryOpen = true },
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
            ) {
                Text(text = stringResource(R.string.inventory_open))
            }
        }
    }
}

/**
 * Câble le [PlayerViewModel] sur les implémentations concrètes Firebase (compte anonyme + Firestore)
 * et l'horloge système. Le cache offline Firestore est configuré en amont (cf. [HexaApplication]).
 */
private val playerViewModelFactory =
    viewModelFactory {
        initializer {
            // Le même dépôt sert l'amorçage (load/save) et l'observation temps réel (observe).
            val repository = FirestorePlayerRepository()
            PlayerViewModel(
                ensurePlayer =
                EnsurePlayerUseCase(
                    auth = FirebaseAuthGateway(),
                    repository = repository,
                    clock = Clock.systemUTC(),
                ),
                repository = repository,
            )
        }
    }
