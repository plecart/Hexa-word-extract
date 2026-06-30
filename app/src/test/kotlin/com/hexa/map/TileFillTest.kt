package com.hexa.map

import com.hexa.config.Element
import com.hexa.ui.theme.HexaGridColors
import com.hexa.ui.theme.ObjectAssets
import com.hexa.world.ElementDeposit
import com.hexa.world.TileContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mapping **pur** état + contenu de tuile → teinte de remplissage rgba (#126). Sans device ni Mapbox :
 * on vérifie la *logique* de coloration (élément le plus rare, neutre, surlignage de la tuile
 * courante) ; la subtilité des teintes et leur fondu dans la carte relèvent de la validation DA à
 * l'œil. Les attentes sont dérivées des **tokens** ([HexaGridColors], [ObjectAssets]) et non de
 * valeurs en dur, pour rester vertes quand la DA affine les teintes sur device.
 */
class TileFillTest : StringSpec({
    "une tuile à gisements prend la teinte de son élément le plus rare, à l'alpha subtil de ressource" {
        // Cendrite (commun) + Échofer (rare) : seule la couleur du plus rare doit ressortir.
        tileFillColor(TileState.NORMALE, tile(Element.CENDRITE, Element.ECHOFER)) shouldBe
            ObjectAssets.of(Element.ECHOFER).color.copy(alpha = HexaGridColors.resourceFillAlpha).toRgba()
    }

    "une tuile sans gisement prend la couleur neutre, distincte d'une tuile à ressource" {
        tileFillColor(TileState.NORMALE, tile()) shouldBe HexaGridColors.emptyTile.toRgba()
        tileFillColor(TileState.NORMALE, tile()) shouldNotBe
            tileFillColor(TileState.NORMALE, tile(Element.CENDRITE))
    }

    "la tuile courante porte le surlignage, indépendamment de son contenu" {
        // Le surlignage prime sur la teinte de ressource : repérable même sur un gisement.
        tileFillColor(TileState.COURANTE, tile(Element.ECHOFER)) shouldBe HexaGridColors.currentTile.toRgba()
        tileFillColor(TileState.COURANTE, tile(Element.ECHOFER)) shouldBe tileFillColor(TileState.COURANTE, tile())
    }

    "le surlignage de la tuile courante est distinct des teintes de ressource" {
        tileFillColor(TileState.COURANTE, tile(Element.ECHOFER)) shouldNotBe
            tileFillColor(TileState.NORMALE, tile(Element.ECHOFER))
    }
})

/** Construit le contenu d'une tuile à partir d'éléments présents (aucun = tuile vide). */
private fun tile(vararg elements: Element): TileContent =
    TileContent(elements.map { ElementDeposit(it, richness = 0.5, ratePerHour = 1) })
