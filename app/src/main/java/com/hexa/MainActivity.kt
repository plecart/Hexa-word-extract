package com.hexa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hexa.map.MapScreen
import com.hexa.ui.theme.HexaTheme
import com.mapbox.common.MapboxOptions

/**
 * Unique activité de l'application (single-activity). Fournit le token public au SDK Mapbox,
 * pose le thème Compose et affiche la carte plein écran.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Le SDK Mapbox lit le token public ici, avant toute instanciation de carte.
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        enableEdgeToEdge()
        setContent {
            HexaTheme {
                MapScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
