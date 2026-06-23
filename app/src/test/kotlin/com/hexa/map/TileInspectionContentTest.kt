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
import com.hexa.ui.theme.HexaTheme
import com.hexa.world.ElementDeposit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tracer bullet de l'**infra de test UI Compose en saveur Robolectric** : prouve de bout en bout que
 * le harnais Compose UI-test (JUnit 4 : [createComposeRule], [RobolectricTestRunner]) cohabite avec
 * Kotest/JUnit 5 sous la même plateforme JUnit (via `junit-vintage-engine`), et qu'un composable se
 * rend et s'assert dans `src/test` sans émulateur ni dépendance Mapbox/GPS.
 *
 * Cible le [TileInspectionContent] (corps du bottom sheet d'inspection, extrait de sa coquille
 * `ModalBottomSheet`). Les assertions portent sur l'**arbre sémantique** — présence/absence de nœuds
 * texte — pas sur un rendu visuel à l'œil, d'où une exécution AFK. Les libellés attendus sont résolus
 * via le [Context] (et non copiés en dur) pour ne pas coupler le test à la formulation des ressources.
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

    @Test
    fun `tuile peuplee affiche le titre, son gisement et le badge tuile courante`() {
        render(
            TileInspection(
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
        render(TileInspection(deposits = emptyList(), isCurrent = false))

        composeRule.onNodeWithText(context.getString(R.string.tile_inspection_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.tile_inspection_here)).assertDoesNotExist()
    }
}
