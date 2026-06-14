package com.hexa.player

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Vérifie l'amorçage du compte : création conforme au premier lancement et **idempotence** au
 * relancement (même uid, aucune réécriture, kit non re-crédité). Adossé aux fakes — pas d'émulateur.
 */
class EnsurePlayerUseCaseTest : StringSpec({
    val uid = PlayerId("uid-123")
    val now = Instant.parse("2026-06-14T10:15:30Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    "premier lancement : crée le document joueur avec le kit de départ" {
        runTest {
            val repository = FakePlayerRepository()
            val useCase = EnsurePlayerUseCase(FakeAuthGateway(uid), repository, clock)

            val result = useCase()

            val created = Player.newPlayer(now)
            result shouldBe IdentifiedPlayer(uid, created)
            repository.stored(uid) shouldBe created
            repository.saved shouldHaveSize 1
        }
    }

    "lancements suivants : réutilise le document existant sans le réécrire" {
        runTest {
            val existing = Player.newPlayer(now).copy(baseCell = "8a1fb46622dffff", inventory = Inventory.EMPTY)
            val repository = FakePlayerRepository(mapOf(uid to existing))
            val useCase = EnsurePlayerUseCase(FakeAuthGateway(uid), repository, clock)

            val result = useCase()

            result shouldBe IdentifiedPlayer(uid, existing)
            repository.saved.shouldBeEmpty()
            repository.stored(uid) shouldBe existing
        }
    }
})
