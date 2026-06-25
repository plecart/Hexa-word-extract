package com.hexa

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre l'**activation du mode immersif** ([enableImmersiveSystemBars]) : le contrôleur d'insets de
 * la fenêtre adopte le comportement « barres transitoires au swipe », contrat dont dépend le rendu
 * carte plein écran avec réapparition au geste (cf. #57). La visibilité effective des barres reste un
 * effet device (validation sur appareil réel) ; ici on vérifie le comportement programmé sur le
 * contrôleur, sans Mapbox/GPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImmersiveModeTest {
    @Test
    fun `active le comportement de barres transitoires au swipe`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        controller.enableImmersiveSystemBars()

        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
    }
}
