package com.hexa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.config.GameConfig
import com.hexa.firstlaunch.FirstLaunchScreen
import com.hexa.inventory.BuildingsScreen
import com.hexa.inventory.ResourcesScreen
import com.hexa.map.LocationPermissionGate
import com.hexa.map.MapScreen
import com.hexa.player.CollectHarvestUseCase
import com.hexa.player.CraftBuildingUseCase
import com.hexa.player.EnsurePlayerUseCase
import com.hexa.player.FirebaseAuthGateway
import com.hexa.player.FirestoreBuildingsRepository
import com.hexa.player.FirestorePlayerRepository
import com.hexa.player.HarvestCalculator
import com.hexa.player.PlaceExtractorUseCase
import com.hexa.player.PlayerUiState
import com.hexa.player.PlayerViewModel
import com.hexa.startup.LoadingScreen
import com.hexa.startup.StartupStage
import com.hexa.startup.startupStage
import com.hexa.ui.theme.HexaAction
import com.hexa.ui.theme.HexaActionBar
import com.hexa.ui.theme.HexaTheme
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Unique activité de l'application (single-activity). Fournit le token public au SDK Mapbox, pose le
 * thème Compose et affiche la carte plein écran avec, par-dessus, la page Ressources ouverte depuis la
 * barre flottante. Au démarrage, l'amorçage silencieux du compte joueur ([PlayerViewModel]) alimente
 * les ressources en temps réel.
 */
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels { playerViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Le SDK Mapbox lit le token public ici, avant toute instanciation de carte.
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        enableEdgeToEdge()
        // Carte en plein écran : barres système masquées (immersif « sticky »), réapparition au swipe.
        WindowCompat.getInsetsController(window, window.decorView).enableImmersiveSystemBars()
        setContent {
            HexaTheme {
                HexaRoot(playerViewModel)
            }
        }
    }
}

/**
 * Racine de l'UI : la **machine à états de démarrage** ([startupStage]). La porte de permission
 * ([LocationPermissionGate]) enveloppe le tout — refus → écran dédié inchangé. Une fois la permission
 * accordée, tant que la position du joueur n'est pas connue, l'**écran de chargement** ([LoadingScreen])
 * masque carte **et** overlays — on ne montre jamais un centre arbitraire ni la pose de base avant le
 * fix. Au premier fix, la bascule vers le jeu se fait en **fondu** ([Crossfade]) ; la carte ([GameScene])
 * apparaît alors centrée sur le joueur.
 */
@Composable
private fun HexaRoot(viewModel: PlayerViewModel) {
    val app = LocalContext.current.applicationContext as HexaApplication
    val playerState by viewModel.state.collectAsStateWithLifecycle()

    LocationPermissionGate(modifier = Modifier.fillMaxSize()) {
        val premierFix by app.premierFix.collectAsStateWithLifecycle()
        val stage = startupStage(premierFix, playerState)
        Crossfade(targetState = stage == StartupStage.LOADING, label = "startup-loading") { isLoading ->
            if (isLoading) {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                GameScene(
                    viewModel = viewModel,
                    playerState = playerState,
                    firstLaunch = stage == StartupStage.FIRST_LAUNCH,
                )
            }
        }
    }
}

/**
 * Scène de jeu (états « fix connu ») : la carte **reste composée dessous** (son état caméra est
 * préservé) et une surcouche s'affiche selon l'état du compte. Tant que la base n'est pas posée
 * ([firstLaunch]), l'**écran de premier lancement** invite à la poser par-dessus la carte ; sinon
 * (base posée, ou amorçage en cours/échoué) ce sont les **pages de jeu** (Ressources, Bâtiments) qui
 * sont superposables. La carte reçoit les bâtiments posés à rendre en 3D ([PlayerViewModel.placedBuildings]).
 */
@Composable
private fun GameScene(viewModel: PlayerViewModel, playerState: PlayerUiState, firstLaunch: Boolean) {
    Box(Modifier.fillMaxSize()) {
        MapScreen(
            placedBuildings = viewModel.placedBuildings,
            extractorStock = viewModel.extractorStock,
            onPlaceExtracteur = viewModel::placeExtracteur,
            modifier = Modifier.fillMaxSize(),
        )

        if (firstLaunch) {
            FirstLaunchScreen(modifier = Modifier.fillMaxSize())
        } else {
            GameOverlays(playerState, onCraftExtracteur = viewModel::craftExtracteur)
        }
    }
}

/** Pages plein écran superposables à la carte, ouvertes depuis la barre flottante. */
private enum class OverlayPanel { RESOURCES, BUILDINGS }

/**
 * Superpose à la carte les pages plein écran de jeu (ressources, bâtiments), une fois la base posée (ou
 * pendant l'amorçage/échec). Une seule page est ouverte à la fois ([open]) ; chacune descend en fondu
 * sur la carte à l'ouverture et remonte à la fermeture. La **barre d'actions** (ancrée en bas, centrée)
 * fait le fondu inverse : elle disparaît dès qu'une page est ouverte et porte une action par page. Le
 * bouton retour système referme la page courante (même transition) plutôt que de quitter l'app.
 *
 * @param playerState état du compte joueur, transmis aux deux pages.
 * @param onCraftExtracteur déclenche le craft d'un extracteur depuis la page Bâtiments.
 */
@Composable
private fun GameOverlays(playerState: PlayerUiState, onCraftExtracteur: () -> Unit) {
    var open by rememberSaveable { mutableStateOf<OverlayPanel?>(null) }

    Box(Modifier.fillMaxSize()) {
        OverlayPage(visible = open == OverlayPanel.RESOURCES) {
            ResourcesScreen(
                state = playerState,
                onClose = { open = null },
                modifier = Modifier.fillMaxSize(),
            )
        }

        OverlayPage(visible = open == OverlayPanel.BUILDINGS) {
            BuildingsScreen(
                state = playerState,
                onClose = { open = null },
                onCraftExtracteur = onCraftExtracteur,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = open == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) {
            HexaActionBar(
                actions =
                listOf(
                    HexaAction(
                        icon = Icons.Outlined.Backpack,
                        contentDescription = stringResource(R.string.resources_open),
                        onClick = { open = OverlayPanel.RESOURCES },
                    ),
                    HexaAction(
                        icon = Icons.Outlined.Factory,
                        contentDescription = stringResource(R.string.buildings_open),
                        onClick = { open = OverlayPanel.BUILDINGS },
                    ),
                ),
            )
        }

        BackHandler(enabled = open != null) { open = null }
    }
}

/**
 * Coquille d'animation commune aux pages superposables : la page descend en fondu sur la carte quand
 * [visible] passe à vrai et remonte en fondu à la fermeture. Factorise la transition partagée par
 * l'inventaire et les bâtiments.
 */
@Composable
private fun OverlayPage(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
    }
}

/**
 * Câble le [PlayerViewModel] sur les implémentations concrètes Firebase (compte anonyme + Firestore)
 * et l'horloge système. Le cache offline Firestore est configuré en amont (cf. [HexaApplication]), qui
 * fournit aussi le contenu de tuile partagé ([HexaApplication.tileContentOf]) au calcul de récolte.
 */
private val playerViewModelFactory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as HexaApplication
            // Le même compte et les mêmes dépôts servent l'amorçage, l'observation temps réel, le craft
            // et la récolte ; l'horloge système est partagée par les cas d'usage à horloge injectée.
            val auth = FirebaseAuthGateway()
            val repository = FirestorePlayerRepository()
            val buildings = FirestoreBuildingsRepository()
            val clock = Clock.systemUTC()
            PlayerViewModel(
                ensurePlayer = EnsurePlayerUseCase(auth = auth, repository = repository, clock = clock),
                repository = repository,
                buildings = buildings,
                craftBuilding = CraftBuildingUseCase(auth = auth, players = repository),
                placeExtractor = PlaceExtractorUseCase(
                    auth = auth,
                    players = repository,
                    buildings = buildings,
                    clock = clock,
                ),
                collectHarvest = CollectHarvestUseCase(
                    auth = auth,
                    players = repository,
                    buildings = buildings,
                    calculator = HarvestCalculator(clock = clock, contentOf = app.tileContentOf),
                ),
                harvestTicks = collectRefreshTicks(),
            )
        }
    }

/**
 * Cadence de récolte : un tick **immédiat** (récolte à l'ouverture) puis un tick toutes les
 * [GameConfig.COLLECT_REFRESH_SECONDS]. Collecté dans le `viewModelScope` (lié au cycle de vie), il
 * **ne crée aucun service ni timer en arrière-plan** : la boucle s'arrête à la destruction du
 * ViewModel et tout le temps app fermée est rattrapé au prochain tick d'ouverture.
 */
private fun collectRefreshTicks(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(GameConfig.COLLECT_REFRESH_SECONDS.seconds)
    }
}
