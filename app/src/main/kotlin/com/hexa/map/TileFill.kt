package com.hexa.map

import androidx.compose.ui.graphics.Color
import com.hexa.ui.theme.HexaGridColors
import com.hexa.ui.theme.ObjectAssets
import com.hexa.world.TileContent
import com.hexa.world.rarestElement
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Teinte de remplissage **rgba** d'une cellule de la grille selon son contenu (#126) **et son
 * éloignement au joueur** (#127). Pure et testable sans Mapbox : c'est la frontière stable entre les
 * contrats de domaine (gisements de la tuile) et la couche de rendu, qui n'a plus qu'à poser la couleur
 * produite.
 *
 * Toutes les tuiles sont traitées à l'identique — **y compris celle sous le joueur** (l'avatar 3D
 * marque déjà sa position ; aucun surlignage de tuile) :
 * - **Tuile à gisement(s)** → couleur d'identité ([ObjectAssets]) de son élément **le plus rare**
 *   ([TileContent.rarestElement]), atténuée à [HexaGridColors.resourceFillAlpha] (teinte subtile).
 * - **Tuile sans gisement** → [HexaGridColors.emptyTile], neutre quasi invisible (fondu dans la carte).
 *
 * L'alpha de cette teinte est **composé** (multiplié) avec le fondu-distance ([GridFade.factorFor]) :
 * plein sur la tuile courante (distance 0), il décroît vers ~0 au bord du rayon rendu pour effacer les
 * tuiles lointaines.
 *
 * @param content contenu de la tuile (ses gisements ; vide si elle ne contient rien).
 * @param distanceRings distance en anneaux H3 de la tuile au joueur (cf. [HexGrid.gridDistance]).
 * @return la couleur au format `rgba(r, g, b, a)` attendu par `fill-color` (alpha porté par la couleur).
 */
fun tileFillColor(content: TileContent, distanceRings: Int): String {
    val tint = content.rarestElement
        ?.let { ObjectAssets.of(it).color.copy(alpha = HexaGridColors.resourceFillAlpha) }
        ?: HexaGridColors.emptyTile
    return tint.copy(alpha = tint.alpha * GridFade.factorFor(distanceRings)).toRgba()
}

/**
 * Couleur Compose → `rgba(r, g, b, a)`, format attendu par `fill-color` Mapbox (alpha dans `[0, 1]`).
 * L'alpha est formaté en [Locale.ROOT] (séparateur décimal `.`) — jamais la locale de la machine, qui
 * produirait une virgule et casserait l'expression Mapbox.
 */
internal fun Color.toRgba(): String {
    fun channel(value: Float): Int = (value * 255).roundToInt()
    return "rgba(${channel(red)}, ${channel(green)}, ${channel(blue)}, ${"%.2f".format(Locale.ROOT, alpha)})"
}
