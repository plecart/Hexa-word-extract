package com.hexa.map

import com.hexa.config.Element
import com.hexa.ui.theme.HexaGridColors
import com.hexa.ui.theme.ObjectAssets
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mapping **pur** contenu de tuile → teinte de remplissage rgba (#126). Sans device ni Mapbox : on
 * vérifie la *logique* de coloration (élément le plus rare, neutre) ; la subtilité des teintes et leur
 * fondu dans la carte relèvent de la validation DA à l'œil. Les attentes sont dérivées des **tokens**
 * ([HexaGridColors], [ObjectAssets]) et non de valeurs en dur, pour rester vertes quand la DA affine
 * les teintes sur device. Toutes les tuiles sont traitées pareil — pas de surlignage de la courante.
 */
class TileFillTest : StringSpec({
    "une tuile à gisements prend la teinte de son élément le plus rare, à l'alpha subtil de ressource" {
        // Cendrite (commun) + Échofer (rare) : seule la couleur du plus rare doit ressortir.
        tileFillColor(tile(Element.CENDRITE, Element.ECHOFER)) shouldBe
            ObjectAssets.of(Element.ECHOFER).color.copy(alpha = HexaGridColors.resourceFillAlpha).toRgba()
    }

    "une tuile sans gisement prend la couleur neutre, distincte d'une tuile à ressource" {
        tileFillColor(tile()) shouldBe HexaGridColors.emptyTile.toRgba()
        tileFillColor(tile()) shouldNotBe tileFillColor(tile(Element.CENDRITE))
    }
})
