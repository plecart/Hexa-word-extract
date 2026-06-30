package com.hexa.ui.theme

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

/** Durée du défilé d'un compteur quand sa valeur change (montée ou baisse), en millisecondes. */
private const val COUNT_ROLL_MILLIS = 400

/**
 * Converteur `Long ↔ AnimationVector1D` rendant une quantité animable : Compose ne fournit de
 * converteur tout fait que pour `Int`, or les stocks d'inventaire sont des `Long`. La valeur transite
 * par le `Float` du vecteur d'animation ; l'aller-retour est **exact tant que la quantité tient dans
 * la mantisse d'un `Float`** (entiers exacts jusqu'à 2^24 ≈ 16,7 M, très au-delà des stocks réels).
 * Au-delà, la valeur au repos peut être arrondie — sans incidence pour un compteur cosmétique.
 */
internal val LongToVector: TwoWayConverter<Long, AnimationVector1D> =
    TwoWayConverter(
        convertToVector = { AnimationVector1D(it.toFloat()) },
        convertFromVector = { it.value.roundToLong() },
    )

/**
 * Compteur dont la valeur affichée **défile** de l'ancienne à la nouvelle quand [amount] change
 * (récolte ou mise à jour temps réel), au lieu de sauter. Micro-feedback de jeu : la montée comme la
 * baisse se lisent. Hors changement, c'est un simple [Text] — le rendu (chiffres tabulaires, accent)
 * vient entièrement de [style] et [color] fournis par l'appelant, ce composable ne fixe que la motion.
 *
 * @param amount quantité courante ; toute variation déclenche le défilé vers la nouvelle valeur.
 * @param style style de texte appliqué au chiffre (l'appelant impose police d'affichage / tabulaire).
 * @param color couleur du chiffre ; [Color.Unspecified] hérite de la couleur de [style].
 * @param modifier agencement laissé à l'appelant.
 */
@Composable
fun AnimatedCount(
    amount: Long,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val rolled by animateValueAsState(
        targetValue = amount,
        typeConverter = LongToVector,
        animationSpec = tween(durationMillis = COUNT_ROLL_MILLIS, easing = FastOutSlowInEasing),
        label = "count",
    )
    Text(rolled.toString(), modifier = modifier, color = color, style = style)
}

/**
 * Aperçu de l'animation du compteur. En **mode interactif** Studio, taper le chiffre bascule la
 * quantité (87 ↔ 142) et déclenche le défilé montée/baisse — une `@Preview` statique ne rendrait que
 * l'état au repos. Le défilé est aussi observable au device, sur une vraie variation de stock.
 */
@Preview(name = "Compteur animé", showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun AnimatedCountPreview() {
    var amount by remember { mutableStateOf(87L) }
    HexaTheme {
        AnimatedCount(
            amount = amount,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier =
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .clickable { amount = if (amount == 87L) 142L else 87L }
                .padding(24.dp),
        )
    }
}
