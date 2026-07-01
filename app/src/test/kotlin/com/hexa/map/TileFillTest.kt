package com.hexa.map

import com.hexa.config.Element
import com.hexa.ui.theme.HexaGridColors
import com.hexa.ui.theme.ObjectAssets
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mapping **pur** contenu de tuile + distance → teinte de remplissage rgba (#126 teinte, #127 fondu).
 * Sans device ni Mapbox : on vérifie la *logique* de coloration (élément le plus rare, neutre) et sa
 * **composition** avec le fondu-distance ([GridFade]) ; la subtilité des teintes et leur fondu dans la
 * carte relèvent de la validation DA à l'œil. Les attentes sont dérivées des **tokens** ([HexaGridColors],
 * [ObjectAssets]) et de [GridFade], non de valeurs en dur, pour rester vertes quand la DA affine les
 * teintes. Toutes les tuiles sont traitées pareil — pas de surlignage de la courante.
 */
class TileFillTest : StringSpec({
    "à la tuile du joueur (distance 0), la teinte de #126 est préservée sans atténuation" {
        // Cendrite (commun) + Échofer (rare) : la couleur du plus rare, à l'alpha de ressource, intacte.
        tileFillColor(tile(Element.CENDRITE, Element.ECHOFER), distanceRings = 0) shouldBe
            ObjectAssets.of(Element.ECHOFER).color.copy(alpha = HexaGridColors.resourceFillAlpha).toRgba()
        // Une tuile sans gisement garde la couleur neutre de #126.
        tileFillColor(tile(), distanceRings = 0) shouldBe HexaGridColors.emptyTile.toRgba()
    }

    "une tuile à gisement reste distincte d'une tuile vide, à distance égale" {
        tileFillColor(tile(Element.CENDRITE), distanceRings = 0) shouldNotBe
            tileFillColor(tile(), distanceRings = 0)
    }

    "l'alpha de teinte se compose avec le fondu-distance : plus loin, plus estompé" {
        val distance = 3
        val rare = ObjectAssets.of(Element.ECHOFER).color.copy(alpha = HexaGridColors.resourceFillAlpha)
        // L'alpha de teinte est multiplié par le facteur de fondu de GridFade à cette distance.
        tileFillColor(tile(Element.ECHOFER), distanceRings = distance) shouldBe
            rare.copy(alpha = rare.alpha * GridFade.factorFor(distance)).toRgba()
    }
})
