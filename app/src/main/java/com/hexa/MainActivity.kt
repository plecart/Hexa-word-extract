package com.hexa

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.map.MapScreen
import com.hexa.player.EnsurePlayerUseCase
import com.hexa.player.FirebaseAuthGateway
import com.hexa.player.FirestorePlayerRepository
import com.hexa.player.PlayerUiState
import com.hexa.player.PlayerViewModel
import com.hexa.ui.theme.HexaTheme
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Unique activité de l'application (single-activity). Fournit le token public au SDK Mapbox, pose le
 * thème Compose et affiche la carte plein écran. Au démarrage, déclenche aussi l'amorçage silencieux
 * du compte joueur ([PlayerViewModel]) et trace l'uid + l'inventaire chargé.
 */
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels { playerViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Le SDK Mapbox lit le token public ici, avant toute instanciation de carte.
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        logPlayerBootstrap()
        enableEdgeToEdge()
        setContent {
            HexaTheme {
                MapScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }

    /**
     * Démonstration de l'amorçage du compte (issue #18) en l'absence d'écran d'inventaire (#22) :
     * trace l'uid et l'inventaire chargé dès que le compte est prêt. À remplacer par l'UI dédiée.
     */
    private fun logPlayerBootstrap() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.state.collect { state ->
                    when (state) {
                        is PlayerUiState.Ready ->
                            Log.i(TAG, "Compte joueur prêt : uid=${state.uid} inventaire=${state.inventory.amounts}")
                        PlayerUiState.Failed -> Log.w(TAG, "Échec de l'amorçage du compte joueur")
                        PlayerUiState.Loading -> Unit
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "HexaPlayer"
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
