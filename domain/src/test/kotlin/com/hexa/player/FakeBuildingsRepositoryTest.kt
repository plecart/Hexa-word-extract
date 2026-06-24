package com.hexa.player

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.time.Instant

/**
 * Verrouille le contrat du double [FakeBuildingsRepository] : un bâtiment placé est rangé sous
 * l'index H3 de sa tuile (un doc par tuile), et chaque écriture est traçable pour les assertions
 * (idempotence, débit nul). C'est la version en mémoire de la sous-collection `buildings/{h3Index}`.
 */
class FakeBuildingsRepositoryTest : StringSpec({
    val uid = PlayerId("uid-123")
    val now = Instant.parse("2026-06-14T10:15:30Z")
    val cell = "8a1fb46622dffff"

    "place range le bâtiment sous l'index H3 de sa tuile et trace l'écriture" {
        runTest {
            val repository = FakeBuildingsRepository()
            val building = PlacedBuilding.base(cell, now)

            repository.place(uid, building)

            repository.buildingsOf(uid) shouldBe mapOf(cell to building)
            repository.saved shouldBe listOf(uid to building)
        }
    }

    "re-poser sur la même tuile écrase le document (un seul bâtiment par tuile)" {
        runTest {
            val repository = FakeBuildingsRepository()
            val first = PlacedBuilding.base(cell, now)
            val second = PlacedBuilding.base(cell, now.plusSeconds(60))

            repository.place(uid, first)
            repository.place(uid, second)

            repository.buildingsOf(uid) shouldBe mapOf(cell to second)
            repository.saved shouldHaveSize 2
        }
    }

    "observe émet la liste à jour des bâtiments du joueur, vide au départ puis à chaque pose" {
        runTest {
            val repository = FakeBuildingsRepository()
            val base = PlacedBuilding.base(cell, now)
            val other = PlacedBuilding.base("8a1fb46622cffff", now)

            repository.observe(uid).first() shouldBe emptyList()

            repository.place(uid, base)
            repository.observe(uid).first() shouldBe listOf(base)

            repository.place(uid, other)
            repository.observe(uid).first() shouldContainExactlyInAnyOrder listOf(base, other)
        }
    }

    "observe isole les bâtiments par joueur" {
        runTest {
            val repository = FakeBuildingsRepository()
            repository.place(uid, PlacedBuilding.base(cell, now))

            repository.observe(PlayerId("autre")).first() shouldBe emptyList()
        }
    }

    "building retourne le bâtiment posé sur la tuile, ou null si elle est libre" {
        runTest {
            val repository = FakeBuildingsRepository()
            val base = PlacedBuilding.base(cell, now)
            repository.place(uid, base)

            repository.building(uid, cell) shouldBe base
            repository.building(uid, "8a1fb46622cffff") shouldBe null
            repository.building(PlayerId("autre"), cell) shouldBe null
        }
    }
})
