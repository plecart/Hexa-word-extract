package com.hexa.ui.theme

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre la **barre d'actions de la carte** ([HexaActionBar]) : chaque [HexaAction] est rendue en item
 * accessible portant son `contentDescription` (lu par TalkBack) et son `onClick` est câblé au tap.
 * C'est le contrat dont dépend l'ouverture de l'inventaire depuis la carte (cf. répercussion #54).
 * Assertions sur l'arbre sémantique, sans dépendance Mapbox/GPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HexaActionBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Libellé d'accessibilité de l'action (déjà résolu côté appelant — la barre le reçoit en clair). */
    private val openInventory = "Ouvrir l'inventaire"

    private fun renderBar(onClick: () -> Unit = {}) {
        composeRule.setContent {
            HexaTheme {
                HexaActionBar(
                    actions = listOf(
                        HexaAction(
                            icon = Icons.Outlined.Backpack,
                            contentDescription = openInventory,
                            onClick = onClick,
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun `l action affiche son content description`() {
        renderBar()

        composeRule.onNodeWithContentDescription(openInventory).assertIsDisplayed()
    }

    @Test
    fun `taper l action invoque son onClick`() {
        var clicked = false
        renderBar(onClick = { clicked = true })

        composeRule.onNodeWithContentDescription(openInventory).performClick()

        assertTrue(clicked)
    }
}
