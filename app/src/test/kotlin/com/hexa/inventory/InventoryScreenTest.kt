package com.hexa.inventory

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.config.Element
import com.hexa.config.GameConfig
import com.hexa.player.BuildingType
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState
import com.hexa.ui.theme.HexaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Couvre l'**écran d'inventaire** ([InventoryScreen]), page plein écran du sac à dos : binding
 * `PlayerUiState` → UI de la liste des ressources. Depuis #104 l'écran ne porte plus que les Ressources
 * (le stock de bâtiments et le craft ont migré sur [BuildingsScreen]) : il n'a donc plus d'onglets ni
 * de bouton « Construire ». La fermeture passe par l'`IconButton` haut-droite ciblé par son
 * `contentDescription`, et les états Loading/Failed rendent leur message. Assertions sur l'arbre
 * sémantique, sans ViewModel ni Firestore (l'écran est piloté par ses paramètres).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class InventoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Inventaire couvrant la recette de l'extracteur (kit de départ). */
    private val affordable = Inventory.of(GameConfig.STARTER_KIT)

    private fun ready(inventory: Inventory) = PlayerUiState.Ready(
        uid = "u",
        inventory = inventory,
        builtBuildings = mapOf(BuildingType.EXTRACTEUR to 0),
        baseCell = null,
    )

    private fun render(state: PlayerUiState, onClose: () -> Unit = {}) {
        composeRule.setContent {
            HexaTheme {
                InventoryScreen(state = state, onClose = onClose)
            }
        }
    }

    @Test
    fun `liste les libelles d elements`() {
        render(ready(affordable))

        composeRule.onNodeWithText(context.getString(labelOf(Element.CENDRITE))).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(labelOf(Element.NYCTITE))).assertIsDisplayed()
    }

    @Test
    fun `ne porte plus le craft de batiment`() {
        render(ready(affordable))

        composeRule.onNodeWithText(context.getString(R.string.inventory_craft_button)).assertDoesNotExist()
    }

    @Test
    fun `bouton de fermeture invoque onClose`() {
        var closed = false
        render(ready(affordable), onClose = { closed = true })

        composeRule.onNodeWithContentDescription(context.getString(R.string.inventory_close)).performClick()

        assertTrue(closed)
    }

    @Test
    fun `etat chargement affiche le message de chargement`() {
        render(PlayerUiState.Loading)

        composeRule.onNodeWithText(context.getString(R.string.inventory_loading)).assertIsDisplayed()
    }

    @Test
    fun `etat echec affiche le message d erreur`() {
        render(PlayerUiState.Failed)

        composeRule.onNodeWithText(context.getString(R.string.inventory_error)).assertIsDisplayed()
    }
}
