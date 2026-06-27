package com.hexa.ui.theme

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre le **compteur animé** ([AnimatedCount]) : à la composition initiale le défilé ne s'est pas
 * encore déclenché — `animateValueAsState` part *à la cible* —, donc le compteur rend sa quantité **au
 * repos**, en clair sur l'arbre sémantique. C'est ce binding `amount` → texte (état déterministe)
 * qu'on verrouille ; le défilé lui-même est une motion cosmétique validée à l'œil (cf. `@Preview`
 * interactive). Assertions sémantiques, sans dépendance Mapbox/GPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AnimatedCountTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `le compteur affiche sa quantite au repos`() {
        composeRule.setContent {
            HexaTheme {
                AnimatedCount(amount = 142L)
            }
        }

        composeRule.onNodeWithText("142").assertIsDisplayed()
    }
}
