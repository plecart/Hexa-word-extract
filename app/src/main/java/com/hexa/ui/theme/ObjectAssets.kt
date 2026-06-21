package com.hexa.ui.theme

import androidx.compose.ui.graphics.Color
import com.hexa.config.Element
import com.hexa.player.BuildingType

/**
 * Les assets d'un objet du jeu, réunis en un point unique : son **master 3D** ([glb]), son **icône
 * 2D** ([icon]) et sa **couleur d'identité** ([color]).
 *
 * Convention durable (cf. issue #51) : chaque objet possède un dossier `assets/objects/<nom>/`
 * contenant `model.glb` + `icon.png`. Le PNG sert toute surface UI (tuiles d'inventaire…) ; le GLB
 * est posé pour le futur rendu 3D sur la carte (Mapbox) mais **pas encore consommé**. La couleur est
 * autorée par la DA — jamais dérivée des pixels du PNG — et reste sourcée depuis [HexaElementColors].
 *
 * @property glb chemin du modèle 3D dans `assets/` (ex. `objects/cendrite/model.glb`), futur usage.
 * @property icon chemin de l'icône PNG dans `assets/` (ex. `objects/cendrite/icon.png`), affichée par
 *   [ElementObject].
 * @property color teinte d'identité, reprise telle quelle de [HexaElementColors].
 */
data class ObjectAsset(val glb: String, val icon: String, val color: Color)

/**
 * Registre `Element → ObjectAsset` : **source unique** de l'identité visuelle (couleur) et des
 * chemins d'assets d'un élément. Absorbe l'ex-table `ElementVisuals` — il n'existe plus de mapping
 * d'identité séparé.
 *
 * Le `when` est **exhaustif sans `else`** : ajouter un élément au domaine casse la compilation tant
 * que ses assets ne sont pas fournis ici — garde-fou contre un objet affiché sans icône ni couleur.
 * Pendant de [com.hexa.inventory.labelOf] côté libellés.
 *
 * Point d'ancrage réutilisable pour tous les rendus d'un objet : icône ([ElementObject],
 * [BuildingObject]) et liseré de tuile ([hexaGlowSurface], couleur). Couvre éléments et bâtiments.
 */
object ObjectAssets {
    /**
     * Les assets de [element].
     *
     * @param element l'élément de domaine dont on veut l'habillage.
     * @return son [ObjectAsset] (chemins + couleur), garanti non nul pour tout élément.
     */
    fun of(element: Element): ObjectAsset = when (element) {
        Element.CENDRITE -> objectAsset("cendrite", HexaElementColors.cendrite)
        Element.GIVRELIN -> objectAsset("givrelin", HexaElementColors.givrelin)
        Element.LITHOSEVE -> objectAsset("lithoseve", HexaElementColors.lithoseve)
        Element.ECHOFER -> objectAsset("echofer", HexaElementColors.echofer)
        Element.NYCTITE -> objectAsset("nyctite", HexaElementColors.nyctite)
    }

    /**
     * Les assets de [building] — même convention `assets/objects/<nom>/` que les éléments.
     *
     * @param building le bâtiment dont on veut l'habillage (icône + couleur d'identité).
     * @return son [ObjectAsset], garanti non nul pour tout bâtiment (`when` exhaustif sans `else`).
     */
    fun of(building: BuildingType): ObjectAsset = when (building) {
        BuildingType.EXTRACTEUR -> objectAsset("extracteur", HexaBuildingColors.extracteur)
    }

    /** Compose l'[ObjectAsset] d'un objet à partir de son dossier [name] et de sa couleur d'identité. */
    private fun objectAsset(name: String, color: Color): ObjectAsset = ObjectAsset(
        glb = "objects/$name/model.glb",
        icon = "objects/$name/icon.png",
        color = color,
    )
}
