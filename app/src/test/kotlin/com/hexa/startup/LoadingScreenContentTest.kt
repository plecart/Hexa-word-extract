package com.hexa.startup

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.ui.theme.HexaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre le **contenu de l'écran de chargement** ([LoadingScreenContent], extrait stateless de son
 * écran) : le statut de localisation est toujours visible, et l'indice d'aide n'apparaît qu'une fois
 * l'attente prolongée signalée ([LoadingScreenContent] paramétré par `showSlowHint`). Assertions sur
 * l'arbre sémantique, sans timer ni GPS — l'indicateur de progression, purement décoratif, n'est pas
 * testé.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LoadingScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun render(showSlowHint: Boolean) {
        composeRule.setContent {
            HexaTheme { LoadingScreenContent(showSlowHint = showSlowHint) }
        }
    }

    @Test
    fun `affiche le logo de marque`() {
        render(showSlowHint = false)

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.loading_logo_description))
            .assertIsDisplayed()
    }

    @Test
    fun `affiche le statut de localisation`() {
        render(showSlowHint = false)

        composeRule.onNodeWithText(context.getString(R.string.loading_status)).assertIsDisplayed()
    }

    @Test
    fun `sans attente prolongee l indice d aide est absent`() {
        render(showSlowHint = false)

        composeRule.onNodeWithText(context.getString(R.string.loading_slow_hint)).assertDoesNotExist()
    }

    @Test
    fun `apres une attente prolongee l indice d aide s affiche`() {
        render(showSlowHint = true)

        composeRule.onNodeWithText(context.getString(R.string.loading_slow_hint)).assertIsDisplayed()
    }
}
