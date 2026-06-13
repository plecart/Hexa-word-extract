package com.hexa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hexa.ui.HomeScreen
import com.hexa.ui.theme.HexaTheme

/**
 * Unique activité de l'application (single-activity). Pose le thème Compose et affiche
 * l'écran d'accueil. Placeholder assumé du MVP : remplacé par la carte dans une tranche
 * ultérieure.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HexaTheme {
                HomeScreen()
            }
        }
    }
}
