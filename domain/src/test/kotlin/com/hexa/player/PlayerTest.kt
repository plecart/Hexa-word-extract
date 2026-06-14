package com.hexa.player

import com.hexa.config.GameConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Vérifie l'amorçage d'un nouveau joueur : kit de départ crédité depuis la configuration centrale,
 * aucune base posée, et stock de bâtiments à zéro — l'état attendu au tout premier lancement.
 */
class PlayerTest : StringSpec({
    "un nouveau joueur reçoit le kit de départ, sans base, avec ses bâtiments à zéro" {
        val createdAt = Instant.parse("2026-06-14T10:15:30Z")

        val player = Player.newPlayer(createdAt)

        player.createdAt shouldBe createdAt
        player.baseCell.shouldBeNull()
        player.inventory shouldBe Inventory.of(GameConfig.STARTER_KIT)
        player.builtBuildings shouldBe mapOf(BuildingType.EXTRACTEUR to 0)
    }
})
