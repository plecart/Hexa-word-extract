package com.hexa.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.hexa.config.Element
import com.hexa.player.BuildingType

/**
 * Rendu visuel d'un **élément** : son icône 2D (`icon.png`), résolue via [ObjectAssets].
 *
 * @param element l'élément à représenter (fixe l'icône via [ObjectAssets]).
 * @param modifier impose la taille et le placement de l'icône (l'appelant fixe la dimension).
 */
@Composable
fun ElementObject(element: Element, modifier: Modifier) = ObjectIcon(ObjectAssets.of(element).icon, modifier)

/**
 * Rendu visuel d'un **bâtiment** : son icône 2D (`icon.png`), résolue via [ObjectAssets]. Pendant de
 * [ElementObject] côté bâtiments (tuile de stock, recette de craft).
 *
 * @param building le bâtiment à représenter (fixe l'icône via [ObjectAssets]).
 * @param modifier impose la taille et le placement de l'icône (l'appelant fixe la dimension).
 */
@Composable
fun BuildingObject(building: BuildingType, modifier: Modifier) = ObjectIcon(ObjectAssets.of(building).icon, modifier)

/**
 * Icône d'un objet du jeu décodée depuis `assets/` ([rememberAssetPainter]).
 *
 * **Point de remplacement unique** : tous les écrans (tuiles d'inventaire aujourd'hui, futur marqueur
 * de carte) passent par ici. Le jour où la carte consomme le `model.glb` (rendu 3D Mapbox), on ne
 * réécrit que ce corps — ni le registre [ObjectAssets], ni les appelants ne bougent.
 *
 * Icône décorative : `contentDescription` nul, le nom de l'objet étant déjà porté par le texte voisin.
 *
 * @param iconPath chemin de l'icône dans `assets/` (cf. [ObjectAsset.icon]).
 * @param modifier taille et placement, fixés par l'appelant.
 */
@Composable
private fun ObjectIcon(iconPath: String, modifier: Modifier) {
    Image(
        painter = rememberAssetPainter(iconPath),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
