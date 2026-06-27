@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.location

import com.hexa.core.geo.LatLng
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * [firstFixPosition] expose le **signal de premier fix GPS** observé par la machine à états de
 * démarrage : `null` tant qu'aucune position filtrée n'est connue, puis la position dès le premier
 * fix (et la dernière connue ensuite). C'est ce passage `null → position` qui fait basculer l'UI de
 * l'écran de chargement à la carte de poursuite — sans jamais instancier la caméra de poursuite.
 *
 * Les collecteurs tournent sur un [UnconfinedTestDispatcher] et le partage démarre en
 * [SharingStarted.Eagerly] : l'amont est souscrit dès la création, sans piloter le temps virtuel.
 */
class FirstFixTest : StringSpec({
    val paris = LatLng(48.8566, 2.3522)
    val next = LatLng(48.8570, 2.3530)

    "vaut null tant qu'aucune position n'est émise" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val positions = MutableSharedFlow<LatLng>()

            val firstFix = firstFixPosition(positions, scope, SharingStarted.Eagerly)

            firstFix.value shouldBe null

            scope.cancel()
        }
    }

    "passe à la première position dès le premier fix" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val positions = MutableSharedFlow<LatLng>()
            val firstFix = firstFixPosition(positions, scope, SharingStarted.Eagerly)

            positions.emit(paris)

            firstFix.value shouldBe paris

            scope.cancel()
        }
    }

    "suit ensuite la dernière position connue" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val positions = MutableSharedFlow<LatLng>()
            val firstFix = firstFixPosition(positions, scope, SharingStarted.Eagerly)

            positions.emit(paris)
            positions.emit(next)

            firstFix.value shouldBe next

            scope.cancel()
        }
    }
})
