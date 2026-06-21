package com.hexa.player

import com.hexa.config.Element
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import java.time.Instant

/**
 * Persistance du craft (cf. PRD #5, user stories 8-11) avec des doubles en mémoire : un craft réussi
 * écrit le document débité+crédité, un craft refusé n'écrit rien. La décision (suffisance, montants)
 * est couverte par [CraftTest] ; ici on vérifie l'orchestration I/O autour de [Craft].
 */
class CraftBuildingUseCaseTest : StringSpec({
    val uid = PlayerId("uid-123")
    val createdAt = Instant.parse("2026-06-14T10:15:30Z")

    "craft réussi : persiste le document avec l'inventaire débité et le stock crédité" {
        runTest {
            val players = FakePlayerRepository(mapOf(uid to Player.newPlayer(createdAt)))
            val useCase = CraftBuildingUseCase(FakeAuthGateway(uid), players)

            useCase(BuildingType.EXTRACTEUR)

            val stored = players.stored(uid)!!
            stored.inventory[Element.CENDRITE] shouldBe 150L // 250 - 100
            stored.inventory[Element.GIVRELIN] shouldBe 60L // 100 - 40
            stored.builtBuildings.getValue(BuildingType.EXTRACTEUR) shouldBe 1
        }
    }

    "craft refusé faute de ressources : aucune écriture, document inchangé" {
        runTest {
            val poor = Player.newPlayer(createdAt)
                .copy(inventory = Inventory.of(mapOf(Element.CENDRITE to 30, Element.GIVRELIN to 40)))
            val players = FakePlayerRepository(mapOf(uid to poor))
            val useCase = CraftBuildingUseCase(FakeAuthGateway(uid), players)

            useCase(BuildingType.EXTRACTEUR)

            players.saved shouldBe emptyList()
            players.stored(uid) shouldBe poor
        }
    }

    "document joueur absent : échoue (amorçage requis avant tout craft)" {
        runTest {
            val useCase = CraftBuildingUseCase(FakeAuthGateway(uid), FakePlayerRepository())

            shouldThrow<IllegalStateException> { useCase(BuildingType.EXTRACTEUR) }
        }
    }
})
