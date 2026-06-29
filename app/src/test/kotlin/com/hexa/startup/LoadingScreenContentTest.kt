package com.hexa.startup

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
 * écran) : le logo et le statut de localisation sont toujours visibles, l'indice d'aide n'apparaît
 * qu'une fois l'attente prolongée signalée (`showSlowHint`), et la barre **binde** son avancement
 * (`progress`) tout en restant **décorative** — elle n'expose aucun pourcentage à l'accessibilité (le
 * 80 % cosmétique serait trompeur, cf. #103). Assertions sur l'arbre sémantique, sans timer ni GPS ;
 * la logique temporelle de remplissage est laissée à `@Preview` + œil humain.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LoadingScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun render(progress: Float = 0f, showSlowHint: Boolean = false) {
        composeRule.setContent {
            HexaTheme { LoadingScreenContent(progress = progress, showSlowHint = showSlowHint) }
        }
    }

    @Test
    fun `affiche le logo de marque`() {
        render()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.loading_logo_description))
            .assertIsDisplayed()
    }

    @Test
    fun `affiche le statut de localisation`() {
        render()

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

    @Test
    fun `la barre reflete l avancement fourni`() {
        render(progress = 0.5f)

        composeRule.onNode(SemanticsMatcher.expectValue(LoadingProgressKey, 0.5f)).assertExists()
    }

    @Test
    fun `la barre n annonce pas de pourcentage a l accessibilite`() {
        render(progress = 0.5f)

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertDoesNotExist()
    }
}
