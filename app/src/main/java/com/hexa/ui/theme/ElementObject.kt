package com.hexa.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.hexa.config.Element

/**
 * Rendu visuel d'un élément : son **icône 2D** (`icon.png`), résolue via le registre [ObjectAssets]
 * et décodée depuis `assets/` ([rememberAssetPainter]).
 *
 * **Point de remplacement unique** : tous les écrans (tuile d'inventaire aujourd'hui, futur marqueur
 * de carte) passent par ce composable. Le jour où la carte consomme le `model.glb` (rendu 3D
 * Mapbox), on ne réécrit que ce corps — ni le registre [ObjectAssets], ni les appelants ne bougent.
 *
 * Icône décorative : `contentDescription` nul, le nom de l'élément étant déjà porté par le texte
 * voisin de la tuile.
 *
 * @param element l'élément à représenter (fixe l'icône via [ObjectAssets]).
 * @param modifier impose la taille et le placement de l'icône (l'appelant fixe la dimension).
 */
@Composable
fun ElementObject(element: Element, modifier: Modifier) {
    Image(
        painter = rememberAssetPainter(ObjectAssets.of(element).icon),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
