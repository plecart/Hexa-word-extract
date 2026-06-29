package com.hexa.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexa.location.CameraState
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.ChaseCameraController
import com.hexa.location.PositionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * État de la caméra de poursuite pour l'écran carte.
 *
 * Orchestre les contrats purs de `:location` : il combine la position suivie ([PositionSource]) au
 * cap et au zoom **pilotés par l'utilisateur**, et confie la décision de pose à
 * [ChaseCameraController]. La caméra reste **verrouillée en permanence sur le joueur** — le centre
 * suit la position, seuls le **cap au glisser** et le **zoom au pincement** varient ; pas de mode
 * libre ni de recentrage. Toute la logique de pose vivant dans `:location`, ce ViewModel reste une
 * glu mince et testable avec des sources factices.
 *
 * Le cap n'est **plus piloté par la boussole** (retiré en #96) : [CompassHeadingSource] et
 * [HeadingSmoother][com.hexa.location.HeadingSmoother] servent désormais l'orientation de l'avatar
 * (#100, cf. [MapScreen]), et non plus la caméra.
 *
 * @param positionSource source de la position suivie (position GPS réelle filtrée et partagée).
 * @param config paramètres de cadrage injectés depuis [MapConfig].
 */
class ChaseCameraViewModel(
    positionSource: PositionSource,
    config: ChaseCameraConfig,
) : ViewModel() {
    /** Politique de poursuite : verrouillée sur le joueur, sans état mutable ni transition de mode. */
    private val controller = ChaseCameraController(config)

    /** Cap piloté par le glisser de rotation ; nord (0°) tant que l'utilisateur n'a pas pivoté. */
    private val userBearing = MutableStateFlow(NORTH_BEARING_DEG)

    /** Zoom choisi au pincement ; `null` tant que l'utilisateur n'a pas ajusté. */
    private val userZoom = MutableStateFlow<Double?>(null)

    /**
     * Pose de caméra à appliquer. `null` **uniquement** tant que la première position n'est pas
     * connue ; dès le premier fix, la caméra suit le joueur en continu. Émet seulement tant qu'il est
     * observé, pour suspendre le replay hors écran.
     */
    val cameraState: StateFlow<CameraState?> =
        combine(
            positionSource.positions(),
            userBearing,
            userZoom,
        ) { position, bearingDeg, zoom ->
            controller.cameraFor(position, bearingDeg, zoom)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MapConfig.SOURCE_STOP_TIMEOUT_MS), null)

    /** À appeler quand le glisser fait pivoter la caméra ; le cap fourni (déjà normalisé) devient la pose. */
    fun onUserBearing(bearingDeg: Double) {
        userBearing.value = bearingDeg
    }

    /** À appeler quand l'utilisateur ajuste le zoom au pincement (sera borné par le contrôleur). */
    fun onUserZoom(zoomLevel: Double) {
        userZoom.value = zoomLevel
    }

    private companion object {
        /** Cap d'amorçage : nord, tant que l'utilisateur n'a pas pivoté la caméra au glisser. */
        const val NORTH_BEARING_DEG = 0.0
    }
}
