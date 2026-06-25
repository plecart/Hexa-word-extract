package com.hexa.map

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.inventory.labelOf
import com.hexa.player.BuildingType
import com.hexa.ui.theme.HexaTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests UI Compose (saveur Robolectric, cf. `tests-ui-compose`) de la pose : le **marqueur « + »**
 * ([ExtractorPlacementMarker]) et le **corps de la liste de pose** ([BuildingPlacementContent], extrait
 * de sa coquille `ModalBottomSheet`). Les assertions portent sur l'arbre sémantique — libellés résolus
 * via le [Context], actions déclenchées au tap — sans rendu visuel ni dépendance Mapbox/GPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BuildingPlacementSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `la liste affiche l extracteur, son stock et pose au tap sur Poser`() {
        var placed = 0
        composeRule.setContent {
            HexaTheme { BuildingPlacementContent(extractorStock = 3, onPlaceExtracteur = { placed++ }) }
        }

        composeRule.onNodeWithText(context.getString(R.string.placement_sheet_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(labelOf(BuildingType.EXTRACTEUR))).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.placement_stock, 3)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.placement_place_button)).performClick()

        placed shouldBe 1
    }

    @Test
    fun `le marqueur expose sa description et s active au tap`() {
        var clicked = 0
        composeRule.setContent {
            HexaTheme { ExtractorPlacementMarker(onClick = { clicked++ }) }
        }

        val marker = composeRule.onNodeWithContentDescription(context.getString(R.string.placement_marker_description))
        marker.assertIsDisplayed()
        marker.performClick()

        clicked shouldBe 1
    }
}
