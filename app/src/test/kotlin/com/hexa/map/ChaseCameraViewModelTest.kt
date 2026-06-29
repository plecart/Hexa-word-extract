@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.map

import com.hexa.core.geo.LatLng
import com.hexa.location.CameraState
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.PositionSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * `cameraState` n'émet que tant qu'il est observé ([kotlinx.coroutines.flow.SharingStarted] WhileSubscribed) :
 * un collecteur de fond active la chaîne sources → contrôleur le temps du test.
 */
private fun CoroutineScope.launchCollector(vm: ChaseCameraViewModel) {
    launch { vm.cameraState.collect {} }
}

/**
 * [ChaseCameraViewModel] est la glu d'app : il combine le flux de position au **cap** et au **zoom**
 * pilotés par l'utilisateur et délègue la pose à [com.hexa.location.ChaseCameraController]. La caméra
 * reste verrouillée sur le joueur ; le cap part au **nord** et n'est plus piloté par la boussole
 * (retiré en #96). On vérifie cette orchestration avec des sources factices, sans device ni Mapbox —
 * la pose elle-même est déjà testée dans `:location`.
 */
class ChaseCameraViewModelTest : StringSpec({
    val config =
        ChaseCameraConfig(
            minPitchDeg = 20.0,
            maxPitchDeg = 70.0,
            followZoom = 17.0,
            minZoom = 14.0,
            maxZoom = 19.0,
        )
    val paris = LatLng(48.8566, 2.3522)

    // viewModelScope s'exécute sur Dispatchers.Main : on le branche sur le planificateur de test.
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    fun viewModel() = ChaseCameraViewModel(
        positionSource = PositionSource { flowOf(paris) },
        config = config,
    )

    "expose la pose centrée sur la position, cap au nord et zoom configuré tant qu'on n'a pas pivoté" {
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()

            // followZoom = 17 → pitch interpolé = 20 + ((17 − 14) / (19 − 14)) × 50 = 50°.
            vm.cameraState.value shouldBe
                CameraState(center = paris, zoomLevel = 17.0, pitchDeg = 50.0, bearingDeg = 0.0)
        }
    }

    "un glisser de rotation répercute le cap sur la pose" {
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()

            vm.onUserBearing(120.0)
            advanceUntilIdle()

            vm.cameraState.value?.bearingDeg shouldBe 120.0
        }
    }

    "un zoom au pincement dans les bornes est répercuté sur la pose" {
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()

            vm.onUserZoom(16.0)
            advanceUntilIdle()

            vm.cameraState.value?.zoomLevel shouldBe 16.0
        }
    }

    "un zoom au pincement hors bornes est ramené dans la plage de jeu" {
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()

            vm.onUserZoom(25.0)
            advanceUntilIdle()

            vm.cameraState.value?.zoomLevel shouldBe 19.0
        }
    }

    "délègue la courbe pitch↔zoom au contrôleur (pilotage du pitch en direct au pincement)" {
        // On vérifie la **délégation** aux deux bornes ; la forme de la courbe (milieu, débordement)
        // est déjà couverte côté `:location` (ChaseCameraControllerTest).
        val vm = viewModel()
        vm.pitchForZoom(14.0) shouldBe 20.0
        vm.pitchForZoom(19.0) shouldBe 70.0
    }
})
