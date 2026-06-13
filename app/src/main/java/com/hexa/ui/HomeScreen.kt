package com.hexa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hexa.BuildConfig
import com.hexa.R
import com.hexa.config.GameConfig
import com.hexa.ui.theme.HexaTheme

/**
 * Écran d'accueil placeholder du MVP. Affiche le nom et la version de l'application
 * (câblage du build) ainsi qu'une constante de [GameConfig] (câblage de la configuration
 * centrale), prouvant la chaîne de bout en bout build → config → UI.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(text = stringResource(R.string.home_version, BuildConfig.VERSION_NAME))
            Text(text = stringResource(R.string.home_h3_resolution, GameConfig.H3_RESOLUTION))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HexaTheme { HomeScreen() }
}
