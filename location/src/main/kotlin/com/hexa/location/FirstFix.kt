package com.hexa.location

import com.hexa.core.geo.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Réduit un flux de [positions] filtrées au **signal de premier fix GPS** : un [StateFlow] valant
 * `null` tant qu'aucune position n'est connue, puis la position dès le premier fix (et la dernière
 * connue ensuite).
 *
 * C'est ce passage `null → position` qu'observe la machine à états de démarrage pour quitter l'écran
 * de chargement : tant que la valeur est `null`, on ne montre **jamais** la carte (donc jamais un
 * centre arbitraire) ; à la première position, la carte de poursuite s'affiche centrée sur le joueur,
 * sa valeur courante servant aussi à **amorcer le viewport** directement sur lui (pas de glissement
 * depuis un centre de repli). Dérivé **sans** instancier la caméra de poursuite.
 *
 * Hot via [stateIn] dans le [scope] fourni (typiquement à l'échelle du processus, comme le partage de
 * position amont) : la dernière valeur est conservée et rejouée aux observateurs qui s'abonnent en
 * cours de route, au travers des recréations de ViewModels.
 *
 * @param positions flux des positions filtrées (cf. [SharedPositionSource]).
 * @param scope portée qui héberge le partage chaud.
 * @param started politique de démarrage du partage (en production
 *   [SharingStarted.WhileSubscribed], alignée sur le partage de position amont).
 */
fun firstFixPosition(positions: Flow<LatLng>, scope: CoroutineScope, started: SharingStarted): StateFlow<LatLng?> =
    positions.stateIn(scope, started, null)
