package com.hexa.player

/**
 * Identifiant **stable** d'un joueur — l'uid du compte Firebase, qui sert aussi d'ID du document
 * `players/{uid}`. Encapsulé dans un type valeur pour ne pas le confondre avec d'autres chaînes
 * (index H3, libellés…) dans les signatures.
 *
 * Cet uid reste stable après un passage ultérieur au SSO (account linking Firebase) : aucune
 * migration de données n'est nécessaire (cf. PRD #5, user story 17).
 *
 * @property value l'uid brut.
 */
@JvmInline
value class PlayerId(val value: String)
