@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.player

import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
 * inventaire) une fois l'amorçage abouti, ou `Failed` s'il échoue. Adossé à des doubles en mémoire ;
 * l'idempotence de l'amorçage est couverte côté [EnsurePlayerUseCase].
 */
class PlayerViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()
    val clock = Clock.fixed(Instant.parse("2026-06-14T10:15:30Z"), ZoneOffset.UTC)

    beforeTest { Dispatchers.setMain(dispatcher) }
    afterTest { Dispatchers.resetMain() }

    "au démarrage, l'état passe de Loading à Ready avec l'uid et l'inventaire du kit de départ" {
        runTest(dispatcher) {
            val useCase =
                EnsurePlayerUseCase(
                    auth = AuthGateway { PlayerId("uid-1") },
                    repository = AbsentPlayerRepository,
                    clock = clock,
                )
            val viewModel = PlayerViewModel(useCase)

            viewModel.state.value shouldBe PlayerUiState.Loading
            advanceUntilIdle()
            viewModel.state.value shouldBe PlayerUiState.Ready("uid-1", Inventory.of(GameConfig.STARTER_KIT))
        }
    }

    "si l'amorçage échoue, l'état passe à Failed" {
        runTest(dispatcher) {
            val useCase =
                EnsurePlayerUseCase(
                    auth = AuthGateway { error("réseau indisponible") },
                    repository = AbsentPlayerRepository,
                    clock = clock,
                )
            val viewModel = PlayerViewModel(useCase)

            advanceUntilIdle()
            viewModel.state.value shouldBe PlayerUiState.Failed
        }
    }
})

/** Dépôt sans document : force le chemin « premier lancement » (création). */
private object AbsentPlayerRepository : PlayerRepository {
    override suspend fun load(id: PlayerId): Player? = null

    override suspend fun save(id: PlayerId, player: Player) = Unit

    override fun observe(id: PlayerId): Flow<Player?> = flowOf(null)
}
