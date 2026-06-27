package com.hexa.inventory

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
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
 * Couvre la **page Bâtiments** ([BuildingsScreen]), page plein écran autonome ouverte depuis la barre
 * flottante : binding `PlayerUiState` → UI du stock d'extracteurs prêts à poser et du craft, dont le
 * bouton « Construire » est **désactivé tant que la recette n'est pas couverte** (garde-fou contre un
 * craft impossible). La fermeture passe par l'`IconButton` haut-droite ciblé par son
 * `contentDescription`, et les états Loading/Failed rendent leur message. Assertions sur l'arbre
 * sémantique, sans ViewModel ni Firestore (la page est pilotée par ses paramètres).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BuildingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Inventaire couvrant la recette de l'extracteur (kit de départ) → craft possible. */
    private val affordable = Inventory.of(GameConfig.STARTER_KIT)

    /** Inventaire vide → aucune ressource de la recette → craft impossible. */
    private val empty = Inventory.of(emptyMap())

    private fun ready(inventory: Inventory, stock: Int = 0) = PlayerUiState.Ready(
        uid = "u",
        inventory = inventory,
        builtBuildings = mapOf(BuildingType.EXTRACTEUR to stock),
        baseCell = null,
    )

    private fun render(state: PlayerUiState, onClose: () -> Unit = {}, onCraft: () -> Unit = {}) {
        composeRule.setContent {
            HexaTheme {
                BuildingsScreen(state = state, onClose = onClose, onCraftExtracteur = onCraft)
            }
        }
    }

    @Test
    fun `affiche la tuile de stock de l extracteur`() {
        render(ready(affordable, stock = 3))

        composeRule.onNodeWithText(context.getString(R.string.building_extracteur)).assertIsDisplayed()
    }

    @Test
    fun `craft active quand la recette est couverte`() {
        render(ready(affordable))

        composeRule.onNodeWithText(context.getString(R.string.inventory_craft_button)).assertIsEnabled()
    }

    @Test
    fun `craft desactive quand la recette n est pas couverte`() {
        render(ready(empty))

        composeRule.onNodeWithText(context.getString(R.string.inventory_craft_button)).assertIsNotEnabled()
    }

    @Test
    fun `taper construire quand la recette est couverte invoque le craft`() {
        var crafted = false
        render(ready(affordable), onCraft = { crafted = true })

        composeRule.onNodeWithText(context.getString(R.string.inventory_craft_button)).performClick()

        assertTrue(crafted)
    }

    @Test
    fun `bouton de fermeture invoque onClose`() {
        var closed = false
        render(ready(affordable), onClose = { closed = true })

        composeRule.onNodeWithContentDescription(context.getString(R.string.buildings_close)).performClick()

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
