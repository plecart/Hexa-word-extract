package com.hexa.inventory

import androidx.annotation.StringRes
import com.hexa.R
import com.hexa.config.Element

/**
 * Libellé localisé affiché pour chaque [Element] dans l'inventaire.
 *
 * Le `when` est **exhaustif sans `else`** : ajouter un élément au domaine casse la compilation tant
 * que son libellé n'est pas fourni ici — garde-fou contre un compteur affiché sans nom.
 */
@StringRes
fun labelOf(element: Element): Int = when (element) {
    Element.CENDRITE -> R.string.element_cendrite
    Element.GIVRELIN -> R.string.element_givrelin
    Element.LITHOSEVE -> R.string.element_lithoseve
    Element.ECHOFER -> R.string.element_echofer
    Element.NYCTITE -> R.string.element_nyctite
}
