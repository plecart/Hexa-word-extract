package com.hexa.map

import com.hexa.config.Element
import com.hexa.core.geo.LatLng
import com.hexa.player.PlacedBuilding
import com.hexa.player.PlacementDecision
import com.hexa.player.PlacementRefusal
import com.hexa.world.ElementDeposit
import com.hexa.world.TileContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * [TileInspectionViewModel] est la glu d'app de l'inspection : il résout la cellule H3 sous le point
 * tapé via la grille, en recalcule le contenu à la volée (jamais stocké), le marque « tuile courante »
 * si elle est sous le joueur et calcule si une **pose d'extracteur** y est possible (et sinon sa
 * raison). On vérifie cette orchestration avec une fausse grille et un faux générateur — sans device ni
 * H3 — : la résolution tap → cellule, l'état du panneau et la décision de pose dérivée.
 */
class TileInspectionViewModelTest : StringSpec({
    "ouvre le panneau avec le contenu de la tuile tapée" {
        val deposits = listOf(ElementDeposit(Element.CENDRITE, richness = 0.7, ratePerHour = 42))
        val vm = viewModel(contentOf = { TileContent(deposits) })

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.deposits shouldBe deposits
    }

    "résout la cellule sous le point tapé via la grille avant d'en lire le contenu" {
        var inspectedCell: Long? = null
        val vm = viewModel(
            contentOf = { cell ->
                inspectedCell = cell
                TileContent(emptyList())
            },
        )

        // La fausse grille mappe la latitude tronquée sur la cellule : 48.0 → 48.
        vm.inspectAt(LatLng(48.0, 2.0))

        inspectedCell shouldBe 48L
    }

    "marque la tuile inspectée comme courante quand c'est celle du joueur" {
        val vm = viewModel(currentTile = MutableStateFlow(48L))

        vm.inspectAt(LatLng(48.0, 2.0))
        vm.inspection.value?.isCurrent?.shouldBeTrue()

        vm.inspectAt(LatLng(49.0, 2.0))
        vm.inspection.value?.isCurrent?.shouldBeFalse()
    }

    "expose un état vide explicite pour une tuile sans gisement" {
        val vm = viewModel()

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.isEmpty?.shouldBeTrue()
    }

    "donne un contenu identique pour deux inspections de la même tuile" {
        // Générateur déterministe : le contenu ne dépend que de la cellule.
        val vm = viewModel(
            contentOf = { cell -> TileContent(listOf(ElementDeposit(Element.GIVRELIN, cell / 100.0, cell.toInt()))) },
        )

        vm.inspectAt(LatLng(48.0, 2.0))
        val first = vm.inspection.value
        vm.inspectAt(LatLng(48.0, 2.0))
        val second = vm.inspection.value

        second shouldBe first
    }

    "juge la pose possible sur la tuile courante, libre et approvisionnée" {
        val vm = viewModel(currentTile = MutableStateFlow(48L), extractorStock = 1)

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.placement shouldBe PlacementDecision.Placeable
    }

    "motive le refus de pose hors de la tuile courante" {
        val vm = viewModel(currentTile = NO_CURRENT, extractorStock = 1)

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.placement shouldBe PlacementDecision.Refused(PlacementRefusal.NOT_CURRENT_TILE)
    }

    "motive le refus de pose sur la tuile courante déjà bâtie" {
        // La fausse grille résout la cellule 48 en index textuel "48" (cf. toH3String).
        val vm = viewModel(
            currentTile = MutableStateFlow(48L),
            placedBuildings = listOf(PlacedBuilding.base("48", NOW)),
            extractorStock = 1,
        )

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.placement shouldBe PlacementDecision.Refused(PlacementRefusal.TILE_OCCUPIED)
    }

    "motive le refus de pose sur la tuile courante libre mais sans stock" {
        val vm = viewModel(currentTile = MutableStateFlow(48L), extractorStock = 0)

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.placement shouldBe PlacementDecision.Refused(PlacementRefusal.NO_STOCK)
    }

    "referme le panneau au dismiss" {
        val vm = viewModel()
        vm.inspectAt(LatLng(48.0, 2.0))

        vm.dismiss()

        vm.inspection.value.shouldBeNull()
    }
})

/** Aucune tuile courante connue (pas encore de fix GPS) : isole les cas qui ne portent pas dessus. */
private val NO_CURRENT = MutableStateFlow<Long?>(null)

/** Instant fixe pour horodater les bâtiments posés des scénarios d'occupation. */
private val NOW = Instant.parse("2026-06-22T08:30:00Z")

/**
 * Fabrique un [TileInspectionViewModel] sur une [FakeInspectionGrid], avec des valeurs par défaut
 * neutres (aucun gisement, aucune tuile courante, aucun bâtiment, aucun stock) : chaque test ne
 * renseigne que les entrées qui portent son scénario.
 */
private fun viewModel(
    contentOf: (Long) -> TileContent = { TileContent(emptyList()) },
    currentTile: StateFlow<Long?> = NO_CURRENT,
    placedBuildings: List<PlacedBuilding> = emptyList(),
    extractorStock: Int = 0,
) = TileInspectionViewModel(
    grid = FakeInspectionGrid(),
    contentOf = contentOf,
    currentTile = currentTile,
    placedBuildings = MutableStateFlow(placedBuildings),
    extractorStock = MutableStateFlow(extractorStock),
)

/**
 * Fausse grille déterministe pour l'inspection : la cellule est la latitude tronquée du point, pour
 * des assertions simples. Les autres opérations ne servent pas l'inspection et sont stubées.
 */
private class FakeInspectionGrid : HexGrid {
    override fun cellAt(position: LatLng): Long = position.latDeg.toLong()

    override fun disk(center: Long, rings: Int): List<Long> = listOf(center)

    override fun outline(cell: Long): List<LatLng> = listOf(LatLng(cell.toDouble(), 0.0))

    override fun centerOf(h3Index: Long): LatLng = LatLng(h3Index.toDouble(), 0.0)

    override fun toH3String(cell: Long): String = cell.toString()
}
