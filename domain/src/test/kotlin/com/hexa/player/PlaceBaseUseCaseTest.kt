package com.hexa.player

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pose de la base offerte (cf. PRD #5, user stories 3-5 ; spec F5). Vérifie le comportement externe
 * avec des doubles : `baseCell` renseigné sur le document joueur, document bâtiment `type = base`
 * créé sur la tuile avec `lastCollectedAt = now`, et **gratuité** (aucun débit d'inventaire ni de
 * stock de bâtiments). L'horloge est injectée pour fixer l'instant de pose de façon testable.
 */
class PlaceBaseUseCaseTest : StringSpec({
    val uid = PlayerId("uid-123")
    val createdAt = Instant.parse("2026-06-14T10:15:30Z")
    val placedAt = Instant.parse("2026-06-20T09:00:00Z")
    val clock = Clock.fixed(placedAt, ZoneOffset.UTC)
    val cell = "8a1fb46622dffff"

    "pose la base : renseigne baseCell sur le document joueur" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt)))
            val useCase = PlaceBaseUseCase(FakeAuthGateway(uid), players, FakeBuildingsRepository(), clock)

            useCase(cell)

            players.stored(uid)?.baseCell shouldBe cell
        }
    }

    "crée le document bâtiment type base avec lastCollectedAt = now sur la tuile" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt)))
            val buildings = FakeBuildingsRepository()

            PlaceBaseUseCase(FakeAuthGateway(uid), players, buildings, clock)(cell)

            buildings.buildingsOf(uid) shouldBe mapOf(cell to PlacedBuilding.base(cell, placedAt))
        }
    }

    "la base est offerte : ni l'inventaire ni le stock de bâtiments ne sont débités" {
        runTest {
            val player = Player.newPlayer(createdAt)
            val players = FakePlayerRepository(mapOf(uid to player))

            PlaceBaseUseCase(FakeAuthGateway(uid), players, FakeBuildingsRepository(), clock)(cell)

            val stored = players.stored(uid)!!
            stored.inventory shouldBe player.inventory
            stored.builtBuildings shouldBe player.builtBuildings
        }
    }
})
