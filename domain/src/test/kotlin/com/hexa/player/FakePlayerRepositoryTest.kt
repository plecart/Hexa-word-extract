@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.player

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.time.Instant

/**
 * Verrouille le contrat d'observation du fake — il doit reproduire la sémantique de
 * `addSnapshotListener` Firestore : émission immédiate de l'instantané courant, puis ré-émission à
 * chaque écriture. C'est ce contrat que les tests du ViewModel temps réel tiennent pour acquis.
 */
class FakePlayerRepositoryTest : StringSpec({
    val uid = PlayerId("uid-1")
    val player = Player.newPlayer(Instant.parse("2026-06-14T10:15:30Z"))

    "observe émet immédiatement le document courant" {
        runTest {
            val repository = FakePlayerRepository(mapOf(uid to player))

            repository.observe(uid).first() shouldBe player
        }
    }

    "observe émet null pour un joueur absent" {
        runTest {
            val repository = FakePlayerRepository()

            repository.observe(uid).first() shouldBe null
        }
    }

    "observe ré-émet la nouvelle valeur après save (mise à jour temps réel)" {
        runTest {
            val repository = FakePlayerRepository(mapOf(uid to player))
            val seen = mutableListOf<Player?>()
            val job = launch { repository.observe(uid).collect { seen += it } }
            advanceUntilIdle()

            val updated = player.copy(inventory = Inventory.of(mapOf(Element.CENDRITE to 999)))
            repository.save(uid, updated)
            advanceUntilIdle()

            seen.last() shouldBe updated
            job.cancel()
        }
    }
})
