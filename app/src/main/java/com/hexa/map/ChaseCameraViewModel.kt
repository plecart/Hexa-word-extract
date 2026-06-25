package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.location.CameraMode
import com.hexa.location.CameraState
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.ChaseCameraController
import com.hexa.location.HeadingSmoother.smoothedHeading
import com.hexa.location.HeadingSource
import com.hexa.location.PositionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * État de la caméra de poursuite pour l'écran carte.
 *
 * Orchestre les contrats purs de `:location` : il combine la position suivie ([PositionSource]) et le
 * cap lissé ([HeadingSource] + [smoothedHeading]), confie la décision de pose à
 * [ChaseCameraController], et relaie les gestes utilisateur (déplacement → mode libre, recentrage,
 * zoom au pincement). Toute la logique de pose vivant dans `:location`, ce ViewModel reste une glu
 * mince et testable avec des sources factices.
 *
 * @param positionSource source de la position suivie (position GPS réelle filtrée et partagée).
 * @param headingSource source du cap brut (boussole).
 * @param config paramètres de cadrage injectés depuis [MapConfig].
 * @param headingSmoothingFactor coefficient de lissage du cap, cf. [HeadingSmoother][com.hexa.location.HeadingSmoother].
 */
class ChaseCameraViewModel(
    positionSource: PositionSource,
    headingSource: HeadingSource,
    config: ChaseCameraConfig,
    headingSmoothingFactor: Double,
) : ViewModel() {
    /** Contrôleur courant : porte le mode (poursuite/libre) ; ses transitions le remplacent. */
    private val controller = MutableStateFlow(ChaseCameraController(config))

    /** Zoom choisi au pincement ; `null` tant que l'utilisateur n'a pas ajusté. */
    private val userZoom = MutableStateFlow<Double?>(null)

    /**
     * Pose de caméra à appliquer, ou `null` en mode libre (la carte reste où l'utilisateur l'a
     * laissée). Émet uniquement tant qu'il est observé, pour suspendre la boussole et le replay hors
     * écran.
     */
    val cameraState: StateFlow<CameraState?> =
        combine(
            positionSource.positions(),
            headingSource.headings().onStart { emit(DEFAULT_HEADING_DEG) }.smoothedHeading(headingSmoothingFactor),
            controller,
            userZoom,
        ) { position, headingDeg, chaseController, zoom ->
            chaseController.cameraFor(position, headingDeg, zoom)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), null)

    /** Mode courant de la caméra — pilote l'affichage du bouton de recentrage. */
    val mode: StateFlow<CameraMode> =
        controller
            .map { it.mode }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CameraMode.FOLLOW)

    /** À appeler quand l'utilisateur déplace la carte au doigt : suspend la poursuite. */
    fun onUserPan() = controller.update { it.releasedByGesture() }

    /**
     * À appeler sur le contrôle de recentrage : restaure le **cadrage d'origine**. Réengage la
     * poursuite **et** efface le zoom au pincement persisté ([userZoom] → `null`), de sorte que la
     * pose reparte du `followZoom` par défaut (cf. [ChaseCameraController.cameraFor]) plutôt que de
     * conserver la dernière distance au sol issue du pincement.
     */
    fun recenter() {
        userZoom.value = null
        controller.update { it.recentered() }
    }

    /** À appeler quand l'utilisateur ajuste le zoom au pincement (sera borné par le contrôleur). */
    fun onUserZoom(zoomLevel: Double) {
        userZoom.value = zoomLevel
    }

    private companion object {
        /** Cap d'amorçage (nord) avant la première mesure de boussole, pour afficher la pose sans attendre. */
        const val DEFAULT_HEADING_DEG = 0.0
    }
}
