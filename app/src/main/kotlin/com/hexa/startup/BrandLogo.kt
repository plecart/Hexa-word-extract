package com.hexa.startup

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.hexa.R

/**
 * Logo de marque du **splash de chargement** : le **point de remplacement unique** du futur asset de
 * marque (#103). Le contenu visuel est aujourd'hui un placeholder (wordmark « HEXA », string
 * [R.string.loading_wordmark]) — l'arrivée du vrai asset ne touchera **que** ce composable.
 *
 * Porte une `contentDescription` **stable** ([R.string.loading_logo_description]) posée via
 * [clearAndSetSemantics] : elle remplace la sémantique du contenu interne, de sorte que le libellé
 * d'accessibilité **survit au remplacement** du placeholder par un visuel (Image, Canvas…) sans
 * dépendre de son contenu propre.
 *
 * @param modifier modifier appliqué à la zone du logo (gabarit libre — dimensions définitives calées
 *   à l'arrivée de l'asset réel).
 */
@Composable
internal fun BrandLogo(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_logo_description)
    Box(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.loading_wordmark),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
