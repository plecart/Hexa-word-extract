package com.hexa.player

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Persistance de la pose d'un extracteur (cf. PRD #4, user stories 5-9) avec des doubles en mémoire.
 * La décision (tuile courante, occupation, stock) est couverte par [PlacementRulesTest] ; ici on
 * vérifie l'orchestration I/O autour de [PlacementRules] : une pose autorisée décrémente le stock et
 * crée le document `extracteur` (`lastCollectedAt = now`), un refus n'écrit **rien** (stock intact),
 * et l'unicité « un bâtiment par tuile » fait échouer proprement une pose sur une tuile bâtie.
 * L'horloge est injectée pour fixer l'instant de pose de façon testable.
 */
class PlaceExtractorUseCaseTest : StringSpec({
    val uid = PlayerId("uid-123")
    val createdAt = Instant.parse("2026-06-14T10:15:30Z")
    val placedAt = Instant.parse("2026-06-22T08:30:00Z")
    val clock = Clock.fixed(placedAt, ZoneOffset.UTC)
    val cell = "8a1fb46622dffff"

    fun playerWithStock(stock: Int): Player =
        Player.newPlayer(createdAt).copy(builtBuildings = mapOf(BuildingType.EXTRACTEUR to stock))

    "pose un extracteur : décrémente le stock et crée le document extracteur avec lastCollectedAt = now" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to playerWithStock(2)))
            val buildings = FakeBuildingsRepository()

            val decision = PlaceExtractorUseCase(FakeAuthGateway(uid), players, buildings, clock)(cell)

            decision shouldBe PlacementDecision.Placeable
            players.stored(uid)!!.builtBuildings.getValue(BuildingType.EXTRACTEUR) shouldBe 1
            buildings.buildingsOf(uid) shouldBe mapOf(cell to PlacedBuilding.extracteur(cell, placedAt))
        }
    }

    "refuse sur une tuile déjà bâtie : aucune écriture, stock intact" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to playerWithStock(2)))
            val buildings = FakeBuildingsRepository()
            val existing = PlacedBuilding.base(cell, placedAt)
            buildings.place(uid, existing)

            val decision = PlaceExtractorUseCase(FakeAuthGateway(uid), players, buildings, clock)(cell)

            decision shouldBe PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)
            players.saved shouldBe emptyList()
            players.stored(uid)!!.builtBuildings.getValue(BuildingType.EXTRACTEUR) shouldBe 2
            buildings.buildingsOf(uid) shouldBe mapOf(cell to existing)
        }
    }

    "refuse quand le stock d'extracteurs est vide : aucune écriture" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to playerWithStock(0)))
            val buildings = FakeBuildingsRepository()

            val decision = PlaceExtractorUseCase(FakeAuthGateway(uid), players, buildings, clock)(cell)

            decision shouldBe PlacementDecision.Refused(PlacementRefusal.NO_STOCK)
            players.saved shouldBe emptyList()
            buildings.buildingsOf(uid) shouldBe emptyMap()
        }
    }

    "document joueur absent : échoue (amorçage requis avant toute pose)" {
        runTest {
            val useCase =
                PlaceExtractorUseCase(FakeAuthGateway(uid), FakePlayerRepository(), FakeBuildingsRepository(), clock)

            shouldThrow<IllegalStateException> { useCase(cell) }
        }
    }
})
