package com.hexa.inventory

import androidx.annotation.StringRes
import com.hexa.R
import com.hexa.player.BuildingType

/**
 * Libellé localisé affiché pour chaque [BuildingType] dans l'inventaire. Pendant de [labelOf] côté
 * éléments.
 *
 * Le `when` est **exhaustif sans `else`** : ajouter un bâtiment au domaine casse la compilation tant
 * que son libellé n'est pas fourni ici — garde-fou contre un stock affiché sans nom.
 */
@StringRes
fun labelOf(building: BuildingType): Int = when (building) {
    BuildingType.EXTRACTEUR -> R.string.building_extracteur
}
