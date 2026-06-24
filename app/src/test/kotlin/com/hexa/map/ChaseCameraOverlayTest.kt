package com.hexa.map

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.location.CameraMode
import com.hexa.ui.theme.HexaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre l'**habillage interactif de la carte de poursuite** ([ChaseCameraOverlay]), extrait de la
 * coquille `MapboxMap` pour être rendable hors carte (convention d'extraction de #75). Le cœur Mapbox
 * (ordre des `MapEffect`, glu de tap) n'est pas rendable en JVM/Robolectric — seul ce bord stateless,
 * piloté par le [CameraMode], l'est. Assertions sur l'arbre sémantique, sans Mapbox/GPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChaseCameraOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val recenterLabel: String
        get() = context.getString(R.string.recenter_camera)

    private fun render(mode: CameraMode, onRecenter: () -> Unit = {}) {
        composeRule.setContent {
            HexaTheme { ChaseCameraOverlay(mode = mode, onRecenter = onRecenter) }
        }
    }

    @Test
    fun `mode libre affiche le bouton de recentrage`() {
        render(CameraMode.FREE)

        composeRule.onNodeWithText(recenterLabel).assertIsDisplayed()
    }

    @Test
    fun `mode poursuite masque le bouton de recentrage`() {
        render(CameraMode.FOLLOW)

        composeRule.onNodeWithText(recenterLabel).assertDoesNotExist()
    }

    @Test
    fun `taper le bouton de recentrage declenche le recentrage`() {
        var recentered = false
        render(CameraMode.FREE, onRecenter = { recentered = true })

        composeRule.onNodeWithText(recenterLabel).performClick()

        assertTrue(recentered)
    }
}
