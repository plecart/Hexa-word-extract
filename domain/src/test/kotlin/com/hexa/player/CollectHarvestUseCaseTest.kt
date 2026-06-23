package com.hexa.player

import com.hexa.config.Element
import com.hexa.world.ElementDeposit
import com.hexa.world.TileContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Récolte paresseuse **persistée** (cf. PRD #5, user stories 12-13) : glu autour du module pur
 * [HarvestCalculator]. Avec des doubles en mémoire (aucun émulateur — le domaine est pur), vérifie le
 * comportement de bout en bout : l'inventaire est crédité du temps écoulé et `lastCollectedAt` avancé
 * sur les documents bâtiments, sans écriture quand rien n'est produit.
 */
class CollectHarvestUseCaseTest : StringSpec({
    val uid = PlayerId("uid-123")
    val createdAt = Instant.parse("2026-06-14T10:15:30Z")
    val now = Instant.parse("2026-06-20T12:00:00Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    val cell = "8a1fb46622dffff"

    /** Générateur de contenu injecté : la tuile [cell] porte du Cendrite à 60 u/h, le reste est vide. */
    val worldWithCendrite: (String) -> TileContent = { c ->
        if (c == cell) {
            TileContent(
                listOf(ElementDeposit(Element.CENDRITE, richness = 1.0, ratePerHour = 60)),
            )
        } else {
            TileContent(emptyList())
        }
    }

    fun buildingPlacedAt(at: Instant) =
        PlacedBuilding(cell = cell, type = PlacedBuildingType.BASE, placedAt = at, lastCollectedAt = at)

    "récolte à l'ouverture : crédite l'inventaire du temps écoulé et persiste le document joueur" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt))) // 250 Cendrite
            val buildings = FakeBuildingsRepository().apply { place(uid, buildingPlacedAt(now.minusSeconds(3600))) }
            val useCase =
                CollectHarvestUseCase(
                    FakeAuthGateway(uid),
                    players,
                    buildings,
                    HarvestCalculator(clock, worldWithCendrite),
                )

            useCase()

            players.stored(uid)!!.inventory[Element.CENDRITE] shouldBe 310L // 250 + 60 (1 h × 60 u/h)
        }
    }

    "récolte : avance lastCollectedAt du bâtiment exactement à now" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt)))
            val buildings = FakeBuildingsRepository().apply { place(uid, buildingPlacedAt(now.minusSeconds(3600))) }

            CollectHarvestUseCase(
                FakeAuthGateway(uid),
                players,
                buildings,
                HarvestCalculator(clock, worldWithCendrite),
            )()

            buildings.buildingsOf(uid).getValue(cell).lastCollectedAt shouldBe now
        }
    }

    "rien produit (aucun temps écoulé) : aucune écriture, ni crédit ni avance de curseur" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt)))
            val buildings = FakeBuildingsRepository().apply { place(uid, buildingPlacedAt(now)) }

            CollectHarvestUseCase(
                FakeAuthGateway(uid),
                players,
                buildings,
                HarvestCalculator(clock, worldWithCendrite),
            )()

            players.saved shouldBe emptyList()
            buildings.buildingsOf(uid).getValue(cell).lastCollectedAt shouldBe now
        }
    }

    "aucun bâtiment posé : aucune écriture (rien à récolter)" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt)))

            CollectHarvestUseCase(
                FakeAuthGateway(uid),
                players,
                FakeBuildingsRepository(),
                HarvestCalculator(clock, worldWithCendrite),
            )()

            players.saved shouldBe emptyList()
        }
    }
})
