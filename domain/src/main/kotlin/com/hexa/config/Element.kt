package com.hexa.config

/**
 * Les cinq éléments collectables, **déclarés par rareté croissante**.
 *
 * Cet ordre est contractuel : les listes de [GameConfig] indexées par rareté
 * (longueurs d'onde, seuils de présence, taux de base) suivent exactement
 * l'ordre des entrées de cet enum — l'indice `i` correspond à `Element.entries[i]`.
 */
enum class Element {
    CENDRITE,
    GIVRELIN,
    LITHOSEVE,
    ECHOFER,
    NYCTITE,
}
