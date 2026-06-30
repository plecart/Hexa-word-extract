package com.hexa.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hexa.R
import com.hexa.ui.theme.HexaTheme
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Délai d'attente sans fix au-delà duquel l'écran ajoute un **indice d'aide** doux (localisation à
 * activer, se placer à l'extérieur). Calé dans la fenêtre ~10–15 s du cadrage : assez long pour ne
 * pas s'afficher sur une acquisition GPS normale, assez court pour rassurer si elle traîne. Jamais
 * d'échec dur : l'écran reste un chargement, jamais une erreur.
 */
private val SLOW_HINT_DELAY = 12.seconds

/**
 * Plafond de la **phase cosmétique** : la barre se remplit jusqu'à 80 % sans attendre le vrai
 * chargement, pour un sentiment de progression immédiat. Les 20 % restants sont pilotés par le fix.
 */
private const val COSMETIC_TARGET = 0.8f

/**
 * Durée **indicative** du remplissage cosmétique 0 → 80 % (décéléré : ralentit en approchant du
 * plafond). À caler à l'œil sur device avec la DA — pas une contrainte fonctionnelle.
 */
private const val COSMETIC_FILL_MILLIS = 2500

/**
 * Durée **indicative** du remplissage final jusqu'à 100 %, depuis la valeur courante au moment du
 * premier fix. À caler à l'œil sur device avec la DA.
 */
private const val FINAL_FILL_MILLIS = 450

/** Largeur de la barre nue (gabarit indicatif, calé à l'œil avec la DA). */
private val BAR_WIDTH = 220.dp

/**
 * Clé sémantique **custom** exposant l'avancement `[0, 1]` de la barre pour les tests UI. Les clés
 * custom ne sont **pas** restituées par TalkBack : elle laisse la barre décorative (cf. [LoadingProgressBar])
 * tout en rendant son binding `progress` observable sur l'arbre sémantique.
 */
internal val LoadingProgressKey = SemanticsPropertyKey<Float>("LoadingProgress")
internal var SemanticsPropertyReceiver.loadingProgress by LoadingProgressKey

/**
 * Écran de **chargement au lancement** : affiché plein écran tant que la barre n'a pas fini de se
 * remplir (cf. [com.hexa.MainActivity]). Masque la carte **et** ses overlays, pour ne jamais laisser
 * voir un centre arbitraire ; à la fin du remplissage, la racine bascule vers la carte en fondu.
 *
 * Glu mince qui gère les deux états temporels de l'écran :
 * - après [SLOW_HINT_DELAY] sans fix, lève `showSlowHint` (indice d'aide) ;
 * - pilote la **barre intelligente** : une phase cosmétique remplit 0 → [COSMETIC_TARGET] sans
 *   attendre le chargement, puis le premier fix ([fixAcquired]) file la barre jusqu'à 100 % **depuis
 *   sa valeur courante** — sans revenir ni marquer le palier 80 %. Un fix déjà présent à l'entrée
 *   donne un balayage 0 → 100 % sur la seule durée finale (aucune latence artificielle). À 100 %,
 *   [onLoadingComplete] signale que le fondu peut se faire — pour que les 20 % finaux soient vus.
 *
 * Le rendu vit dans [LoadingScreenContent], stateless et testable ; la logique temporelle est
 * validée par `@Preview` + œil humain (comme le timer [SLOW_HINT_DELAY]).
 *
 * @param fixAcquired `true` dès que la position du joueur est connue (premier fix GPS) ; pilote les
 *   20 % finaux de la barre.
 * @param onLoadingComplete appelé une fois la barre remplie à 100 % — déclenche la bascule vers la carte.
 */
@Composable
fun LoadingScreen(fixAcquired: Boolean, onLoadingComplete: () -> Unit, modifier: Modifier = Modifier) {
    var showSlowHint by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SLOW_HINT_DELAY)
        showSlowHint = true
    }

    val progress = remember { Animatable(0f) }
    val fixAcquiredNow = rememberUpdatedState(fixAcquired)
    val onComplete = rememberUpdatedState(onLoadingComplete)
    LaunchedEffect(Unit) {
        // Phase cosmétique : remplissage décéléré vers 80 %, lancé tout de suite, annulable au fix.
        val cosmetic = launch {
            progress.animateTo(COSMETIC_TARGET, tween(COSMETIC_FILL_MILLIS, easing = LinearOutSlowInEasing))
        }
        // Phase réelle : au premier fix, on file jusqu'à 100 % DEPUIS la valeur courante (fix rapide →
        // pas de palier 80 %, pas de retour arrière). Fix déjà là → attente immédiate, balayage 0 → 100 %.
        snapshotFlow { fixAcquiredNow.value }.first { it }
        cosmetic.cancelAndJoin()
        progress.animateTo(1f, tween(FINAL_FILL_MILLIS, easing = FastOutSlowInEasing))
        onComplete.value()
    }

    LoadingScreenContent(progress = progress.value, showSlowHint = showSlowHint, modifier = modifier)
}

/**
 * Contenu sans état du splash : fond anthracite plein écran, centré de haut en bas — **logo de
 * marque** ([BrandLogo], placeholder en attendant un asset dédié), **statut** « Localisation en
 * cours… » (+ indice d'aide quand [showSlowHint]), puis la **barre nue** ([LoadingProgressBar]) liée
 * à [progress]. Pas de panneau DA autour de la zone (#103).
 *
 * Applique [safeDrawingPadding] — cohérent avec [com.hexa.firstlaunch.FirstLaunchPanel] — pour rester
 * hors des barres système transitoires du mode immersif.
 *
 * @param progress avancement `[0, 1]` de la barre de chargement.
 * @param showSlowHint affiche l'indice d'aide d'attente prolongée (piloté par [LoadingScreen]).
 */
@Composable
internal fun LoadingScreenContent(progress: Float, showSlowHint: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            BrandLogo()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
            LoadingProgressBar(progress = progress)
        }
    }
}

/**
 * Barre de chargement **purement décorative** : son avancement n'est **jamais** annoncé en pourcentage
 * à TalkBack (le 80 % cosmétique serait trompeur) — l'info honnête est portée par le texte de statut.
 * [clearAndSetSemantics] efface la sémantique par défaut de l'indicateur (qui annoncerait le
 * pourcentage) et n'expose que la clé custom [LoadingProgressKey], lue par les tests mais pas par
 * l'accessibilité.
 *
 * @param progress avancement `[0, 1]` du remplissage.
 */
@Composable
private fun LoadingProgressBar(progress: Float) {
    Box(modifier = Modifier.clearAndSetSemantics { loadingProgress = progress }) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.width(BAR_WIDTH),
        )
    }
}

@Preview
@Composable
private fun LoadingScreenEarlyPreview() {
    HexaTheme { LoadingScreenContent(progress = 0.35f, showSlowHint = false) }
}

@Preview
@Composable
private fun LoadingScreenSlowHintPreview() {
    HexaTheme { LoadingScreenContent(progress = COSMETIC_TARGET, showSlowHint = true) }
}

@Preview
@Composable
private fun LoadingScreenCompletePreview() {
    HexaTheme { LoadingScreenContent(progress = 1f, showSlowHint = false) }
}
