package com.hexa.map

import android.Manifest
import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.hexa.R
import com.hexa.ui.theme.HexaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Couvre la **porte de permission de localisation** ([LocationPermissionGate]) : le binding entre
 * l'état de `ACCESS_FINE_LOCATION` et l'UI. Permission accordée → le contenu protégé (la carte) est
 * rendu ; refusée → l'état explicite (message + bouton « Autoriser ») est rendu et le contenu protégé
 * masqué. L'état de permission est piloté par les shadows Robolectric (sans dialogue système réel) ;
 * les assertions portent sur l'arbre sémantique, sans Mapbox/GPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocationPermissionGateTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** L'`Application` : pilote l'état de permission via son shadow, et résout les libellés attendus. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Sentinelle rendue par le slot `granted` : sa présence atteste que la porte a laissé passer. */
    private val protectedContent = "contenu-protege"

    private fun renderGate() {
        composeRule.setContent {
            HexaTheme {
                LocationPermissionGate { Text(protectedContent) }
            }
        }
    }

    @Test
    fun `permission accordee laisse passer le contenu protege`() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        renderGate()

        composeRule.onNodeWithText(protectedContent).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.location_permission_retry)).assertDoesNotExist()
    }

    @Test
    fun `permission refusee affiche message et bouton, masque le contenu protege`() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        renderGate()

        composeRule.onNodeWithText(app.getString(R.string.location_permission_rationale)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.location_permission_retry)).assertIsDisplayed()
        composeRule.onNodeWithText(protectedContent).assertDoesNotExist()
    }
}
