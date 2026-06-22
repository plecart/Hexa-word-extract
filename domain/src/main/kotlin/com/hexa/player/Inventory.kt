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

    /**
     * Inventaire **débité** de [costs] (unités par élément, ex. une recette de craft). Les éléments
     * absents de [costs] sont laissés intacts.
     *
     * Opération de bas niveau : l'appelant garantit que chaque compteur couvre son coût (cf. [Craft],
     * qui vérifie la suffisance avant de débiter). Sans ce contrôle, un compteur passerait négatif.
     *
     * @param costs montant à retrancher par élément.
     * @return un nouvel inventaire, les cinq compteurs préservés.
     */
    fun minus(costs: Map<Element, Int>): Inventory =
        Inventory(amounts.mapValues { (element, amount) -> amount - (costs[element]?.toLong() ?: 0L) })

    /**
     * Inventaire **crédité** de [gains] (unités par élément, ex. une récolte). Les éléments absents de
     * [gains] sont laissés intacts. Opération inverse de [minus] ; les gains sont des [Long] car une
     * récolte hors ligne accumule sur de longues durées, au-delà de la capacité d'un [Int].
     *
     * @param gains montant à ajouter par élément.
     * @return un nouvel inventaire, les cinq compteurs préservés.
     */
    fun plus(gains: Map<Element, Long>): Inventory =
        Inventory(amounts.mapValues { (element, amount) -> amount + (gains[element] ?: 0L) })

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
