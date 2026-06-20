package com.hexa.ui.theme

import androidx.compose.ui.graphics.Color
import com.hexa.config.Element

/**
 * Identité visuelle **stable** d'un élément, indépendante de la façon dont on le dessine.
 *
 * Ne porte que la donnée d'identité (la [color]) ; le *rendu* (bloc placeholder aujourd'hui, modèle
 * 3D texturé demain) vit dans un point unique séparé. Ce découplage est délibéré : remplacer le
 * placeholder par un vrai objet 3D ne touchera que le rendu, jamais ce mapping ni les écrans qui le
 * consomment.
 *
 * @property color teinte d'identité de l'élément, reprise telle quelle de [HexaElementColors].
 */
data class ElementVisual(val color: Color)

/**
 * Mapping `Element → identité visuelle` ([ElementVisual]), **exhaustif sans `else`** : ajouter un
 * élément au domaine casse la compilation tant que son identité n'est pas fournie ici — garde-fou
 * contre un élément affiché sans couleur.
 *
 * Pendant de [com.hexa.inventory.labelOf] côté libellés. Les couleurs sont **consommées** depuis
 * [HexaElementColors] (jamais redéfinies), pour une source de vérité unique calée à l'œil par la DA.
 * Point d'ancrage réutilisable pour tous les rendus d'un élément (tuile d'inventaire aujourd'hui,
 * marqueur de carte demain).
 */
object ElementVisuals {
    /**
     * Identité visuelle de [element].
     *
     * @param element l'élément de domaine dont on veut l'habillage.
     * @return son [ElementVisual] (couleur d'identité), garanti non nul pour tout élément.
     */
    fun of(element: Element): ElementVisual = when (element) {
        Element.CENDRITE -> ElementVisual(HexaElementColors.cendrite)
        Element.GIVRELIN -> ElementVisual(HexaElementColors.givrelin)
        Element.LITHOSEVE -> ElementVisual(HexaElementColors.lithoseve)
        Element.ECHOFER -> ElementVisual(HexaElementColors.echofer)
        Element.NYCTITE -> ElementVisual(HexaElementColors.nyctite)
    }
}
