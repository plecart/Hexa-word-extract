package com.hexa.firstlaunch

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.ui.theme.HexaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre le **panneau de premier lancement** ([FirstLaunchPanel], extrait stateless de son écran) :
 * le binding entre les deux états dérivés (peut-on poser, attend-on la position) et l'UI. Le bouton
 * « Poser ma base » n'est actif que lorsque la pose est possible ; tant que la position est attendue,
 * un indice l'explique. Assertions sur l'arbre sémantique, sans ViewModel ni Firestore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FirstLaunchPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun render(canPlace: Boolean, awaitingPosition: Boolean, onPlaceBase: () -> Unit = {}) {
        composeRule.setContent {
            HexaTheme {
                FirstLaunchPanel(
                    canPlace = canPlace,
                    awaitingPosition = awaitingPosition,
                    onPlaceBase = onPlaceBase,
                )
            }
        }
    }

    @Test
    fun `pose possible active le bouton et masque l indice d attente`() {
        render(canPlace = true, awaitingPosition = false)

        composeRule.onNodeWithText(context.getString(R.string.first_launch_place_base)).assertIsEnabled()
        composeRule.onNodeWithText(context.getString(R.string.first_launch_awaiting_position)).assertDoesNotExist()
    }

    @Test
    fun `position attendue desactive le bouton et affiche l indice d attente`() {
        render(canPlace = false, awaitingPosition = true)

        composeRule.onNodeWithText(context.getString(R.string.first_launch_place_base)).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.first_launch_awaiting_position)).assertIsDisplayed()
    }

    @Test
    fun `taper poser la base invoque le rappel`() {
        var placed = false
        render(canPlace = true, awaitingPosition = false, onPlaceBase = { placed = true })

        composeRule.onNodeWithText(context.getString(R.string.first_launch_place_base)).performClick()

        assertTrue(placed)
    }
}
