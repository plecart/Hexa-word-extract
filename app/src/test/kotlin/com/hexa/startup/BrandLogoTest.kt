package com.hexa.startup

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.ui.theme.HexaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre le **logo de chargement** ([BrandLogo]) : son seul comportement observable est de porter une
 * `contentDescription` **stable** (`loading_logo_description`), pensée pour survivre au remplacement du
 * contenu visuel placeholder par l'asset de marque réel (cf. #103). On teste ce contrat sémantique, pas
 * le rendu visuel — laissé à `@Preview` + œil humain.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BrandLogoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `expose la contentDescription stable du logo`() {
        composeRule.setContent {
            HexaTheme { BrandLogo() }
        }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.loading_logo_description))
            .assertIsDisplayed()
    }
}
