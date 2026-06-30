package com.hexa.player

import kotlin.enums.enumEntries

/**
 * Clé de champ Firestore d'une valeur d'enum : son nom en **minuscules**.
 *
 * Codec **unique** de la convention enum ↔ clé de champ, partagé par tous les mappers de document
 * (cf. [PlayerDocumentMapper], [BuildingDocumentMapper]) : une valeur d'enum se sérialise par son nom
 * en minuscules et se relit ([toEnumOrNull]) en comparant sur cette même forme. Encodage et décodage
 * sont donc rigoureusement **symétriques** — fin de l'asymétrie de casse historique (encoder en
 * minuscules, décoder en comparant le nom en majuscules) qui n'était sûre que tant que `lowercase()` se
 * relisait. Centraliser ici garantit qu'un changement de convention ne touche qu'un seul endroit, et
 * que tous les enums persistés (éléments, types de bâtiments en stock ou posés) la partagent.
 *
 * Ces libellés sont **contractuels** (un futur serveur les relira) ; `PlayerDocumentMapperTest` et
 * `BuildingDocumentMapperTest` verrouillent la forme exacte écrite pour chaque enum.
 */
val Enum<*>.fieldKey: String get() = name.lowercase()

/**
 * Valeur d'enum [E] relue depuis sa clé de champ Firestore ([fieldKey]) — inverse exact de [fieldKey] —
 * ou `null` si la clé ne correspond à aucune valeur connue.
 *
 * Le repli sur `null` assure la **forward-compat** : une clé écrite par une version future du schéma
 * (valeur d'enum ajoutée, renommée ou retirée) est écartée sans lever, à charge de l'appelant de
 * décider du repli (ignorer le document, valeur par défaut…).
 *
 * Utilise [enumEntries] (liste mise en cache) plutôt que `enumValues` : pas d'allocation de tableau
 * par appel, ce qui compte pour les décodages dans le flux temps réel des bâtiments.
 */
inline fun <reified E : Enum<E>> String.toEnumOrNull(): E? {
    val key = this
    return enumEntries<E>().firstOrNull { it.fieldKey == key }
}
