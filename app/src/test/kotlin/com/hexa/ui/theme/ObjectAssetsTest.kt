package com.hexa.ui.theme

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Verrouille le registre `Element → ObjectAsset` ([ObjectAssets]), source unique d'identité **et**
 * de chemins d'assets depuis la fusion de l'ex-`ElementVisuals` :
 *
 * - **identité couleur** : une teinte distincte par élément, reprise telle quelle de
 *   [HexaElementColors] dans l'ordre de rareté (ex-`ElementVisualsTest`) ;
 * - **convention d'assets** : chaque élément pointe un dossier `objects/<nom>/` propre contenant
 *   `model.glb` + `icon.png`, le `<nom>` étant le nom de l'élément en minuscules ;
 * - **présence sur disque** : les deux fichiers de chaque paire existent réellement (garde-fou
 *   contre une entrée de registre orpheline).
 *
 * L'exhaustivité (« pas d'absent ») est garantie par le compilateur (`when` sans `else`) et exercée
 * ici en parcourant `Element.entries`.
 */
class ObjectAssetsTest : StringSpec({
    val assets = Element.entries.map { ObjectAssets.of(it) }

    "chaque élément a une couleur d'identité distincte" {
        assets.map { it.color }.toSet() shouldHaveSize Element.entries.size
    }

    "la couleur reprend le token canonique, dans l'ordre de rareté" {
        assets.map { it.color } shouldBe HexaElementColors.all
    }

    "chaque élément pointe son dossier objects/<nom> avec model.glb et icon.png" {
        Element.entries.forEach { element ->
            val folder = "objects/${element.name.lowercase()}"
            val asset = ObjectAssets.of(element)
            asset.glb shouldBe "$folder/model.glb"
            asset.icon shouldBe "$folder/icon.png"
        }
    }

    "les fichiers de chaque paire d'assets existent sur disque" {
        assets.forEach { asset ->
            assetFile(asset.glb).isFile shouldBe true
            assetFile(asset.icon).isFile shouldBe true
        }
    }
})

/**
 * Résout un chemin d'asset (relatif à `assets/`) vers un fichier réel, quel que soit le répertoire
 * de travail du runner Gradle (racine du module `app/` ou racine du dépôt).
 */
private fun assetFile(path: String): File {
    val candidates = listOf("src/main/assets/$path", "app/src/main/assets/$path").map(::File)
    return candidates.firstOrNull(File::isFile) ?: candidates.first()
}
