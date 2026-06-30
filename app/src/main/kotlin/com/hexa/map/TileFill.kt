package com.hexa.map

import androidx.compose.ui.graphics.Color
import com.hexa.ui.theme.HexaGridColors
import com.hexa.ui.theme.ObjectAssets
import com.hexa.world.TileContent
import com.hexa.world.rarestElement
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Teinte de remplissage **rgba** d'une cellule de la grille selon son état et son contenu (#126).
 * Pure et testable sans Mapbox : c'est la frontière stable entre les contrats de domaine (état de
 * tuile, gisements) et la couche de rendu, qui n'a plus qu'à poser la couleur produite.
 *
 * Priorité des rôles :
 * 1. **Tuile courante** ([TileState.COURANTE]) → surlignage [HexaGridColors.currentTile], **quel que
 *    soit son contenu** : il prime sur la teinte de ressource pour que le joueur se repère toujours.
 * 2. **Tuile à gisement(s)** → couleur d'identité ([ObjectAssets]) de son élément **le plus rare**
 *    ([TileContent.rarestElement]), atténuée à [HexaGridColors.resourceFillAlpha] (teinte subtile).
 * 3. **Tuile sans gisement** → [HexaGridColors.emptyTile], neutre quasi invisible (fondu dans la carte).
 *
 * @param state état visuel de la cellule (courante ou normale).
 * @param content contenu de la tuile (ses gisements ; vide si elle ne contient rien).
 * @return la couleur au format `rgba(r, g, b, a)` attendu par `fill-color` (alpha porté par la couleur).
 */
fun tileFillColor(state: TileState, content: TileContent): String = when (state) {
    TileState.COURANTE -> HexaGridColors.currentTile
    TileState.NORMALE ->
        content.rarestElement
            ?.let { ObjectAssets.of(it).color.copy(alpha = HexaGridColors.resourceFillAlpha) }
            ?: HexaGridColors.emptyTile
}.toRgba()

/**
 * Couleur Compose → `rgba(r, g, b, a)`, format attendu par `fill-color` Mapbox (alpha dans `[0, 1]`).
 * L'alpha est formaté en [Locale.ROOT] (séparateur décimal `.`) — jamais la locale de la machine, qui
 * produirait une virgule et casserait l'expression Mapbox.
 */
internal fun Color.toRgba(): String {
    fun channel(value: Float): Int = (value * 255).roundToInt()
    return "rgba(${channel(red)}, ${channel(green)}, ${channel(blue)}, ${"%.2f".format(Locale.ROOT, alpha)})"
}
