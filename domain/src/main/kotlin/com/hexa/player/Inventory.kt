package com.hexa.player

import com.hexa.config.Element

/**
 * Inventaire du joueur : un compteur, en unités, pour **chacun** des cinq [Element].
 *
 * Les cinq éléments sont toujours présents (invariant garanti par construction) afin que tout
 * consommateur — affichage temps réel, récolte, craft — puisse lire n'importe quel compteur sans
 * gérer l'absence. Les compteurs sont des [Long] : un idle game accumule sur de longues durées hors
 * ligne, au-delà de la capacité d'un [Int].
 *
 * Instancier via [of] ou [EMPTY] plutôt que le constructeur, pour bénéficier de la normalisation.
 *
 * @property amounts compteur par élément ; contient exactement les cinq [Element].
 */
data class Inventory(val amounts: Map<Element, Long>) {
    init {
        require(amounts.keys == ELEMENTS) {
            "Un inventaire doit couvrir exactement les cinq éléments, reçu : ${amounts.keys}"
        }
    }

    /** Compteur de l'[element] donné (jamais nul : l'invariant garantit sa présence). */
    operator fun get(element: Element): Long = amounts.getValue(element)

    companion object {
        private val ELEMENTS: Set<Element> = Element.entries.toSet()

        /** Inventaire neuf : les cinq compteurs à zéro. */
        val EMPTY: Inventory = of(emptyMap())

        /**
         * Construit un inventaire depuis une carte **partielle** (ex. [com.hexa.config.GameConfig.STARTER_KIT]) :
         * les éléments fournis prennent leur valeur, les absents sont complétés à zéro.
         */
        fun of(counts: Map<Element, Int>): Inventory =
            Inventory(Element.entries.associateWith { (counts[it] ?: 0).toLong() })
    }
}
