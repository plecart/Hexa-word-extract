package com.hexa.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.ui.theme.HexaTheme
import com.hexa.ui.theme.hexaGlowSurface
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Délai d'attente sans fix au-delà duquel l'écran ajoute un **indice d'aide** doux (localisation à
 * activer, se placer à l'extérieur). Calé dans la fenêtre ~10–15 s du cadrage : assez long pour ne
 * pas s'afficher sur une acquisition GPS normale, assez court pour rassurer si elle traîne. Jamais
 * d'échec dur : l'écran reste un chargement, jamais une erreur.
 */
private val SLOW_HINT_DELAY = 12.seconds

/**
 * Écran de **chargement au lancement** : affiché plein écran tant que la position du joueur n'est pas
 * connue (cf. machine à états [startupStage]). Masque la carte **et** ses overlays, pour ne jamais
 * laisser voir un centre arbitraire ; à la première position, la racine bascule vers la carte en
 * fondu (cf. [com.hexa.MainActivity]).
 *
 * Glu mince qui gère le seul état temporel de l'écran : après [SLOW_HINT_DELAY] sans fix, lève
 * `showSlowHint`. Le rendu vit dans [LoadingScreenContent], stateless et testable.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    var showSlowHint by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SLOW_HINT_DELAY)
        showSlowHint = true
    }
    LoadingScreenContent(showSlowHint = showSlowHint, modifier = modifier)
}

/**
 * Contenu sans état du splash : fond anthracite plein écran, **logo de marque** ([BrandLogo],
 * placeholder en attendant un asset dédié), puis un panneau DA ([hexaGlowSurface]) portant
 * l'indicateur de progression cyan et le statut « Localisation en cours… ». Quand [showSlowHint], un
 * indice d'aide s'ajoute sous le statut.
 *
 * Applique [safeDrawingPadding] — cohérent avec [com.hexa.firstlaunch.FirstLaunchPanel] — pour rester
 * hors des barres système transitoires du mode immersif.
 *
 * @param showSlowHint affiche l'indice d'aide d'attente prolongée (piloté par [LoadingScreen]).
 */
@Composable
internal fun LoadingScreenContent(showSlowHint: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            BrandLogo()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .hexaGlowSurface(shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.loading_status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (showSlowHint) {
                    Text(
                        text = stringResource(R.string.loading_slow_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoadingScreenAwaitingPreview() {
    HexaTheme { LoadingScreenContent(showSlowHint = false) }
}

@Preview
@Composable
private fun LoadingScreenSlowHintPreview() {
    HexaTheme { LoadingScreenContent(showSlowHint = true) }
}
