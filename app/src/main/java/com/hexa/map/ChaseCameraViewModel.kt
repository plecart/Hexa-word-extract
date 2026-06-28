package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * État de la caméra de poursuite pour l'écran carte.
 *
 * Orchestre les contrats purs de `:location` : il combine la position suivie ([PositionSource]) et le
 * cap lissé ([HeadingSource] + [smoothedHeading]), confie la décision de pose à
 * [ChaseCameraController], et relaie le zoom au pincement. La caméra reste **verrouillée en
 * permanence sur le joueur** — pas de mode libre ni de recentrage. Toute la logique de pose vivant
 * dans `:location`, ce ViewModel reste une glu mince et testable avec des sources factices.
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
    /** Politique de poursuite : verrouillée sur le joueur, sans état mutable ni transition de mode. */
    private val controller = ChaseCameraController(config)

    /** Zoom choisi au pincement ; `null` tant que l'utilisateur n'a pas ajusté. */
    private val userZoom = MutableStateFlow<Double?>(null)

    /**
     * Pose de caméra à appliquer. `null` **uniquement** tant que la première position n'est pas
     * connue ; dès le premier fix, la caméra suit le joueur en continu. Émet seulement tant qu'il est
     * observé, pour suspendre la boussole et le replay hors écran.
     */
    val cameraState: StateFlow<CameraState?> =
        combine(
            positionSource.positions(),
            headingSource.headings().onStart { emit(DEFAULT_HEADING_DEG) }.smoothedHeading(headingSmoothingFactor),
            userZoom,
        ) { position, headingDeg, zoom ->
            controller.cameraFor(position, headingDeg, zoom)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), null)

    /** À appeler quand l'utilisateur ajuste le zoom au pincement (sera borné par le contrôleur). */
    fun onUserZoom(zoomLevel: Double) {
        userZoom.value = zoomLevel
    }

    private companion object {
        /** Cap d'amorçage (nord) avant la première mesure de boussole, pour afficher la pose sans attendre. */
        const val DEFAULT_HEADING_DEG = 0.0
    }
}
