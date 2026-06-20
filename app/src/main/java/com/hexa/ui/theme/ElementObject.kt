package com.hexa.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.hexa.config.Element
import kotlin.math.min

/** Demi-largeur du cube, en fraction du côté de la zone de dessin. */
private const val HALF_WIDTH_RATIO = 0.34f

/** Hauteur des faces latérales, en fraction du côté de la zone de dessin. */
private const val SIDE_HEIGHT_RATIO = 0.30f

/**
 * Rendu visuel d'un élément : aujourd'hui un **bloc cube placeholder** ombré à sa couleur d'identité
 * ([ElementVisuals]), en attendant un vrai modèle 3D texturé.
 *
 * **Point de remplacement unique** : tous les écrans (tuile d'inventaire aujourd'hui, futur marqueur
 * de carte) passent par ce composable. Le jour où arrive un modèle 3D + texture, on ne réécrit que ce
 * corps — ni le mapping [ElementVisuals], ni les appelants ne bougent.
 *
 * @param element l'élément à représenter.
 * @param modifier impose la taille et le placement du bloc (l'appelant fixe la dimension).
 */
@Composable
fun ElementObject(element: Element, modifier: Modifier) {
    val faces = cubeFacesOf(ElementVisuals.of(element).color)
    Canvas(modifier) { drawIsometricCube(faces) }
}

/**
 * Dessine les trois faces visibles d'un cube en projection isométrique, centré dans la zone de
 * dessin : losange supérieur (face [CubeFaces.top]) coiffant deux parallélogrammes
 * ([CubeFaces.left] / [CubeFaces.right]).
 */
private fun DrawScope.drawIsometricCube(faces: CubeFaces) {
    val side = min(size.width, size.height)
    val halfWidth = side * HALF_WIDTH_RATIO
    val sideHeight = side * SIDE_HEIGHT_RATIO
    val centerX = size.width / 2f
    val originY = (size.height - (halfWidth + sideHeight)) / 2f

    val apex = Offset(centerX, originY)
    val rightCorner = Offset(centerX + halfWidth, originY + halfWidth / 2f)
    val leftCorner = Offset(centerX - halfWidth, originY + halfWidth / 2f)
    val frontTop = Offset(centerX, originY + halfWidth)
    val rightBottom = Offset(rightCorner.x, rightCorner.y + sideHeight)
    val leftBottom = Offset(leftCorner.x, leftCorner.y + sideHeight)
    val frontBottom = Offset(centerX, frontTop.y + sideHeight)

    drawPath(polygonOf(listOf(apex, rightCorner, frontTop, leftCorner)), faces.top)
    drawPath(polygonOf(listOf(rightCorner, rightBottom, frontBottom, frontTop)), faces.right)
    drawPath(polygonOf(listOf(leftCorner, leftBottom, frontBottom, frontTop)), faces.left)
}

/** Construit un polygone fermé passant par les sommets donnés, dans l'ordre. */
private fun polygonOf(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}
