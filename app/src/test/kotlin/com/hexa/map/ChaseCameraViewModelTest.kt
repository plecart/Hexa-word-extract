@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hexa.map

import com.hexa.core.geo.LatLng
import com.hexa.location.CameraMode
import com.hexa.location.CameraState
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.HeadingSource
import com.hexa.location.PositionSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
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
 * [ChaseCameraViewModel] est la glu d'app : il combine le flux de position et le flux de cap lissé,
 * délègue la pose à [com.hexa.location.ChaseCameraController] et relaie les gestes (déplacement,
 * recentrage, zoom). On vérifie cette orchestration avec des sources factices, sans device ni
 * Mapbox — la pose elle-même est déjà testée dans `:location`.
 */
class ChaseCameraViewModelTest : StringSpec({
    val config = ChaseCameraConfig(pitchDeg = 60.0, followZoom = 17.0, minZoom = 14.0, maxZoom = 19.0)
    val paris = LatLng(48.8566, 2.3522)

    // viewModelScope s'exécute sur Dispatchers.Main : on le branche sur le planificateur de test.
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    fun viewModel(rawHeadingDeg: Double = 90.0) = ChaseCameraViewModel(
        positionSource = PositionSource { flowOf(paris) },
        headingSource = HeadingSource { flowOf(rawHeadingDeg) },
        config = config,
        // Facteur 1 : le cap lissé adopte immédiatement le brut, simplifiant l'assertion.
        headingSmoothingFactor = 1.0,
    )

    "en poursuite, expose la pose centrée sur la position au cap lissé et au zoom configuré" {
        runTest {
            val vm = viewModel(rawHeadingDeg = 90.0)
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()

            vm.cameraState.value shouldBe
                CameraState(center = paris, zoomLevel = 17.0, pitchDeg = 60.0, bearingDeg = 90.0)
        }
    }

    "un déplacement manuel passe en mode libre et libère la caméra" {
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()

            vm.onUserPan()
            advanceUntilIdle()

            vm.mode.value shouldBe CameraMode.FREE
            vm.cameraState.value.shouldBeNull()
        }
    }

    "le recentrage réengage la poursuite" {
        runTest {
            val vm = viewModel(rawHeadingDeg = 90.0)
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()
            vm.onUserPan()
            advanceUntilIdle()

            vm.recenter()
            advanceUntilIdle()

            vm.mode.value shouldBe CameraMode.FOLLOW
            vm.cameraState.value shouldBe
                CameraState(center = paris, zoomLevel = 17.0, pitchDeg = 60.0, bearingDeg = 90.0)
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

    "le recentrage efface le zoom au pincement et restaure le cadrage par défaut" {
        runTest {
            val vm = viewModel(rawHeadingDeg = 90.0)
            backgroundScope.launchCollector(vm)
            advanceUntilIdle()
            vm.onUserZoom(16.0)
            advanceUntilIdle()

            vm.recenter()
            advanceUntilIdle()

            vm.cameraState.value?.zoomLevel shouldBe 17.0
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
})
