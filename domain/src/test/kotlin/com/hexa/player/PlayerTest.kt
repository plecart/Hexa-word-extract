package com.hexa.player

import com.hexa.config.GameConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Vérifie l'amorçage d'un nouveau joueur (kit de départ, sans base, stock à zéro) et l'**invariant de
 * stock** : `builtBuildings` couvre toujours tous les [BuildingType], si bien que les opérations de
 * stock ([Player.incrementStock]/[Player.decrementStock]/[Player.stockOf]) ne rencontrent jamais une
 * clé absente — symétrie avec l'invariant de [Inventory].
 */
class PlayerTest : StringSpec({
    val createdAt = Instant.parse("2026-06-14T10:15:30Z")

    "un nouveau joueur reçoit le kit de départ, sans base, avec ses bâtiments à zéro" {
        val player = Player.newPlayer(createdAt)

        player.createdAt shouldBe createdAt
        player.baseCell.shouldBeNull()
        player.inventory shouldBe Inventory.of(GameConfig.STARTER_KIT)
        player.builtBuildings shouldBe mapOf(BuildingType.EXTRACTEUR to 0)
    }

    "un stock qui ne couvre pas tous les types de bâtiment est rejeté à la construction" {
        shouldThrow<IllegalArgumentException> {
            Player.newPlayer(createdAt).copy(builtBuildings = emptyMap())
        }
    }

    "incrementStock crédite +1 le type visé et n'altère pas le reste du document" {
        val player = Player.newPlayer(createdAt)

        val credited = player.incrementStock(BuildingType.EXTRACTEUR)

        credited.stockOf(BuildingType.EXTRACTEUR) shouldBe 1
        credited.inventory shouldBe player.inventory
        credited.createdAt shouldBe player.createdAt
    }

    "decrementStock débite -1 le type visé" {
        val player = Player.newPlayer(createdAt).copy(builtBuildings = mapOf(BuildingType.EXTRACTEUR to 2))

        player.decrementStock(BuildingType.EXTRACTEUR).stockOf(BuildingType.EXTRACTEUR) shouldBe 1
    }
})
