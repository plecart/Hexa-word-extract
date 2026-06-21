@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.player

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Vérifie l'état observable exposé par le ViewModel : `Loading` au départ, puis `Ready` (uid +
 * inventaire) une fois l'amorçage abouti, ou `Failed` s'il échoue — et surtout que toute mise à jour
 * du document joueur **se reflète en continu** dans l'inventaire, sans action de l'utilisateur.
 * Adossé à des doubles en mémoire ; l'idempotence de l'amorçage est couverte par [EnsurePlayerUseCase].
 */
class PlayerViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()
    val clock = Clock.fixed(Instant.parse("2026-06-14T10:15:30Z"), ZoneOffset.UTC)
    val uid = PlayerId("uid-1")

    beforeTest { Dispatchers.setMain(dispatcher) }
    afterTest { Dispatchers.resetMain() }

    "au démarrage, l'état passe de Loading à Ready avec l'uid et l'inventaire du kit de départ" {
        runTest(dispatcher) {
            val starter = Player.newPlayer(clock.instant())
            val useCase = EnsurePlayerUseCase(AuthGateway { uid }, AbsentPlayerRepository, clock)
            val viewModel = PlayerViewModel(
                useCase,
                AbsentPlayerRepository,
                ObservableBuildingsRepository(),
                craftFor(AbsentPlayerRepository),
            )

            viewModel.state.value shouldBe PlayerUiState.Loading
            advanceUntilIdle()
            viewModel.state.value shouldBe
                PlayerUiState.Ready("uid-1", starter.inventory, starter.builtBuildings, baseCell = null)
        }
    }

    "si l'amorçage échoue, l'état passe à Failed" {
        runTest(dispatcher) {
            val useCase =
                EnsurePlayerUseCase(AuthGateway { error("réseau indisponible") }, AbsentPlayerRepository, clock)
            val viewModel = PlayerViewModel(
                useCase,
                AbsentPlayerRepository,
                ObservableBuildingsRepository(),
                craftFor(AbsentPlayerRepository),
            )

            advanceUntilIdle()
            viewModel.state.value shouldBe PlayerUiState.Failed
        }
    }

    "une mise à jour du document joueur se reflète dans l'inventaire sans action de l'utilisateur" {
        runTest(dispatcher) {
            val initial = Player.newPlayer(clock.instant())
            val repository = ObservablePlayerRepository(initial)
            val useCase = EnsurePlayerUseCase(AuthGateway { uid }, repository, clock)
            val viewModel = PlayerViewModel(useCase, repository, ObservableBuildingsRepository(), craftFor(repository))
            advanceUntilIdle()

            viewModel.state.value shouldBe
                PlayerUiState.Ready("uid-1", initial.inventory, initial.builtBuildings, baseCell = null)

            val credited = initial.copy(inventory = Inventory.of(mapOf(Element.CENDRITE to 9_999)))
            repository.emit(credited)
            advanceUntilIdle()

            viewModel.state.value shouldBe
                PlayerUiState.Ready("uid-1", credited.inventory, credited.builtBuildings, baseCell = null)
        }
    }

    "un craft d'extracteur débite l'inventaire et crédite le stock, reflété sans action" {
        runTest(dispatcher) {
            val initial = Player.newPlayer(clock.instant()) // 250 Cendrite, 100 Givrelin, 0 extracteur
            val repository = ObservablePlayerRepository(initial)
            val useCase = EnsurePlayerUseCase(AuthGateway { uid }, repository, clock)
            val viewModel = PlayerViewModel(useCase, repository, ObservableBuildingsRepository(), craftFor(repository))
            advanceUntilIdle()

            viewModel.craftExtracteur()
            advanceUntilIdle()

            val ready = viewModel.state.value as PlayerUiState.Ready
            ready.inventory[Element.CENDRITE] shouldBe 150L
            ready.inventory[Element.GIVRELIN] shouldBe 60L
            ready.builtBuildings.getValue(BuildingType.EXTRACTEUR) shouldBe 1
        }
    }

    "un bâtiment posé dans la sous-collection alimente placedBuildings" {
        runTest(dispatcher) {
            val initial = Player.newPlayer(clock.instant())
            val repository = ObservablePlayerRepository(initial)
            val buildings = ObservableBuildingsRepository()
            val useCase = EnsurePlayerUseCase(AuthGateway { uid }, repository, clock)
            val viewModel = PlayerViewModel(useCase, repository, buildings, craftFor(repository))
            advanceUntilIdle()

            viewModel.placedBuildings.value shouldBe emptyList()

            val base = PlacedBuilding.base("8a1fb46622dffff", clock.instant())
            buildings.place(uid, base)
            advanceUntilIdle()

            viewModel.placedBuildings.value shouldBe listOf(base)
        }
    }

    "une erreur du flux temps réel après chargement conserve le dernier inventaire" {
        runTest(dispatcher) {
            val initial = Player.newPlayer(clock.instant())
            val repository = FailingObserveRepository(initial)
            val useCase = EnsurePlayerUseCase(AuthGateway { uid }, repository, clock)
            val viewModel = PlayerViewModel(useCase, repository, ObservableBuildingsRepository(), craftFor(repository))
            advanceUntilIdle()

            viewModel.state.value shouldBe
                PlayerUiState.Ready("uid-1", initial.inventory, initial.builtBuildings, baseCell = null)
        }
    }
})

/** Cas d'usage de craft câblé sur [repository] et l'uid de test, pour construire le ViewModel. */
private fun craftFor(repository: PlayerRepository) = CraftBuildingUseCase(AuthGateway { PlayerId("uid-1") }, repository)

/** Dépôt sans document : force le chemin « premier lancement » (création) et n'émet jamais de doc. */
private object AbsentPlayerRepository : PlayerRepository {
    override suspend fun load(id: PlayerId): Player? = null

    override suspend fun save(id: PlayerId, player: Player) = Unit

    override fun observe(id: PlayerId): Flow<Player?> = flowOf(null)
}

/** Dépôt en mémoire dont [emit] simule une écriture distante (récolte, autre appareil). */
private class ObservablePlayerRepository(initial: Player) : PlayerRepository {
    private val stream = MutableStateFlow<Player?>(initial)

    override suspend fun load(id: PlayerId): Player? = stream.value

    override suspend fun save(id: PlayerId, player: Player) {
        stream.value = player
    }

    override fun observe(id: PlayerId): Flow<Player?> = stream.asStateFlow()

    fun emit(player: Player) {
        stream.value = player
    }
}

/** Sous-collection de bâtiments en mémoire : [place] pousse sur le flux observé (un doc par tuile). */
private class ObservableBuildingsRepository(initial: List<PlacedBuilding> = emptyList()) : BuildingsRepository {
    private val stream = MutableStateFlow(initial)

    override suspend fun place(id: PlayerId, building: PlacedBuilding) {
        stream.value = stream.value.filterNot { it.cell == building.cell } + building
    }

    override fun observe(id: PlayerId): Flow<List<PlacedBuilding>> = stream.asStateFlow()
}

/** Dépôt dont le flux émet l'instantané puis échoue, pour simuler un incident du listener Firestore. */
private class FailingObserveRepository(private val initial: Player) : PlayerRepository {
    override suspend fun load(id: PlayerId): Player = initial

    override suspend fun save(id: PlayerId, player: Player) = Unit

    override fun observe(id: PlayerId): Flow<Player?> = flow {
        emit(initial)
        throw RuntimeException("listener perdu")
    }
}
