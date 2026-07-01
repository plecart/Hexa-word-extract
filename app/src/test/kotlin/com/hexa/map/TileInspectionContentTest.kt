package com.hexa.map

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.config.Element
import com.hexa.inventory.labelOf
import com.hexa.player.PlacementDecision
import com.hexa.player.PlacementRefusal
import com.hexa.ui.theme.HexaTheme
import com.hexa.world.ElementDeposit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests UI Compose (saveur Robolectric, `src/test`) du corps du bottom sheet d'inspection
 * ([TileInspectionContent], extrait de sa coquille `ModalBottomSheet`). Les assertions portent sur
 * l'**arbre sémantique** — présence/absence de nœuds texte — pas sur un rendu visuel à l'œil, d'où une
 * exécution AFK. Les libellés attendus sont résolus via le [Context] (et non copiés en dur) pour ne pas
 * coupler le test à la formulation des ressources.
 *
 * Couvre le contenu (titre, gisement, badge « tuile courante », état vide) et la **ligne de statut de
 * pose** (#110) : chaque état de [PlacementDecision] rend son libellé, y compris « Pose possible » et
 * sur une tuile vide.
 */
// Application nue : un test de composable isolé n'a pas besoin de HexaApplication, dont l'onCreate
// initialise Firebase/H3/GPS (indisponibles et hors sujet ici). Convention des tests UI Compose.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TileInspectionContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Rend le corps du panneau dans le thème de l'app, prêt pour les assertions sémantiques. */
    private fun render(inspection: TileInspection) {
        composeRule.setContent {
            HexaTheme { TileInspectionContent(inspection) }
        }
    }

    /** Inspection minimale : seul le [placement] est requis, le reste par défaut neutre (tuile vide). */
    private fun inspection(
        placement: PlacementDecision,
        deposits: List<ElementDeposit> = emptyList(),
        isCurrent: Boolean = false,
    ) = TileInspection(deposits = deposits, isCurrent = isCurrent, placement = placement)

    @Test
    fun `tuile peuplee affiche le titre, son gisement et le badge tuile courante`() {
        render(
            inspection(
                placement = PlacementDecision.Placeable,
                deposits = listOf(ElementDeposit(Element.CENDRITE, richness = 0.82, ratePerHour = 52)),
                isCurrent = true,
            ),
        )

        composeRule.onNodeWithText(context.getString(R.string.tile_inspection_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(labelOf(Element.CENDRITE))).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.tile_inspection_here)).assertIsDisplayed()
    }

    @Test
    fun `tuile vide affiche l etat vide et masque le badge tuile courante`() {
        render(inspection(placement = PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)))

        composeRule.onNodeWithText(context.getString(R.string.tile_inspection_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.tile_inspection_here)).assertDoesNotExist()
    }

    @Test
    fun `pose possible affiche la ligne pose possible, meme sur une tuile vide`() {
        render(inspection(placement = PlacementDecision.Placeable))

        composeRule.onNodeWithText(context.getString(R.string.placement_status_placeable)).assertIsDisplayed()
    }

    @Test
    fun `refus hors tuile courante affiche sa ligne`() {
        render(inspection(placement = PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)))

        composeRule.onNodeWithText(context.getString(R.string.placement_status_not_current_tile)).assertIsDisplayed()
    }

    @Test
    fun `refus tuile occupee affiche sa ligne`() {
        render(inspection(placement = PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)))

        composeRule.onNodeWithText(context.getString(R.string.placement_status_tile_occupied)).assertIsDisplayed()
    }

    @Test
    fun `refus sans stock affiche sa ligne`() {
        render(inspection(placement = PlacementDecision.Refused(PlacementRefusal.NO_STOCK)))

        composeRule.onNodeWithText(context.getString(R.string.placement_status_no_stock)).assertIsDisplayed()
    }
}
