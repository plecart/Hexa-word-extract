package com.hexa.player

import com.hexa.config.Element
import com.hexa.world.TileContent
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Résultat d'un calcul de récolte : ce qu'il faut **créditer** et ce qu'il faut **persister**.
 *
 * @property gains total à ajouter à l'inventaire, par élément (cf. [Inventory.plus]) ; ne contient que
 *   les éléments effectivement crédités (montant > 0), vide si aucun bâtiment n'a produit d'unité.
 * @property collected bâtiments dont la récolte a été réglée — leur [PlacedBuilding.lastCollectedAt]
 *   est avancé à l'instant du calcul. **Seuls** ces bâtiments sont à réécrire : ceux qui n'ont rien
 *   produit en sont absents, leur curseur inchangé (cf. [HarvestCalculator]).
 */
data class HarvestResult(
    val gains: Map<Element, Long>,
    val collected: List<PlacedBuilding>,
)

/**
 * Cœur **pur** de la récolte paresseuse (cf. PRD #5, user stories 12-13) — aucune I/O : une liste de
 * bâtiments en entrée, un [HarvestResult] en sortie. L'horloge et le générateur de contenu sont
 * **injectés**, si bien que le calcul est déterministe et rejouable — portable tel quel côté serveur
 * (post-MVP), où il serait la seule source de vérité de l'économie.
 *
 * **Modèle « régler les comptes jusqu'à maintenant ».** Pour chaque bâtiment, on lit le temps écoulé
 * depuis sa dernière récolte ([PlacedBuilding.lastCollectedAt]) et, pour chaque gisement de sa tuile
 * (vitesse recalculée à la volée par [contentOf], jamais stockée), on crédite
 * `vitesse × écoulé`, **tronqué à l'unité** (l'inventaire est en unités entières). Le curseur n'est
 * avancé à `now` **que** si au moins une unité entière a été produite ; sinon il est laissé en place.
 *
 * Cette règle « avancer seulement si l'on crédite » est à la fois plus correcte et plus économe que
 * « toujours avancer à `now` » :
 * - **hors ligne** (longue durée écoulée, un seul calcul à la réouverture) : le crédit est exact,
 *   c'est le cœur de la fonctionnalité ;
 * - **app ouverte** (recalcul toutes les [com.hexa.config.GameConfig.COLLECT_REFRESH_SECONDS]) : un
 *   tick produit souvent moins d'une unité (ex. 60 u/h × 30 s = 0,5) ; en **ne** réglant pas, on
 *   laisse l'accumulation franchir le seuil au tick suivant — sans cette préservation, tronquer puis
 *   avancer à `now` à chaque tick figerait les compteurs à zéro en ligne. Régler à vide n'écrirait
 *   d'ailleurs rien d'utile (idempotent) ;
 * - un gisement à vitesse nulle (élément rare arrondi à 0 u/h, cf. [com.hexa.world.ElementDeposit])
 *   ou une tuile sans gisement ne crédite jamais — le bâtiment reste alors hors du résultat.
 *
 * Le reste (perte du résidu sous-unité des éléments lents d'une tuile au moment où un élément rapide
 * la fait régler) est un détail d'équilibrage, hors périmètre au MVP (taux provisoires, cf. PRD #5).
 *
 * @property clock horloge fournissant l'instant de référence du calcul (`now`), injectée pour la
 *   testabilité (`Clock.fixed` en test).
 * @property contentOf générateur de contenu de tuile : index H3 **textuel** d'une tuile → ses
 *   gisements. Injecté (le natif H3 vit côté `:app`) ; en production il combine la grille et le
 *   [com.hexa.world.WorldGenerator].
 */
class HarvestCalculator(
    private val clock: Clock,
    private val contentOf: (String) -> TileContent,
) {
    /**
     * Calcule la récolte de tous les [buildings] à l'instant courant de l'[clock].
     *
     * @param buildings bâtiments du joueur (cf. [BuildingsRepository.observe]).
     * @return les gains à créditer et les bâtiments dont le curseur est à réécrire (cf. [HarvestResult]).
     */
    fun collect(buildings: List<PlacedBuilding>): HarvestResult {
        val now = clock.instant()
        val totals = mutableMapOf<Element, Long>()
        val collected = buildings.mapNotNull { building ->
            val gains = gainsOf(building, now)
            if (gains.isEmpty()) return@mapNotNull null
            gains.forEach { (element, amount) -> totals.merge(element, amount, Long::plus) }
            building.copy(lastCollectedAt = now)
        }
        return HarvestResult(gains = totals, collected = collected)
    }

    /** Gains entiers du [building] au regard du temps écoulé jusqu'à [now] ; vide si rien d'entier. */
    private fun gainsOf(building: PlacedBuilding, now: Instant): Map<Element, Long> {
        val elapsedSeconds = Duration.between(building.lastCollectedAt, now).seconds.coerceAtLeast(0)
        return contentOf(building.cell).deposits
            .associate { it.element to it.ratePerHour.toLong() * elapsedSeconds / SECONDS_PER_HOUR }
            .filterValues { it > 0 }
    }

    private companion object {
        /** Les vitesses sont en unités par heure ; le temps écoulé est mesuré en secondes. */
        const val SECONDS_PER_HOUR = 3600L
    }
}
