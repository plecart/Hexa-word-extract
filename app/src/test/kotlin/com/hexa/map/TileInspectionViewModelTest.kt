package com.hexa.map

import com.hexa.config.Element
import com.hexa.core.geo.LatLng
import com.hexa.world.ElementDeposit
import com.hexa.world.TileContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [TileInspectionViewModel] est la glu d'app de l'inspection : il résout la cellule H3 sous le point
 * tapé via la grille, en recalcule le contenu à la volée (jamais stocké) et le marque « tuile
 * courante » si elle est sous le joueur. On vérifie cette orchestration avec une fausse grille et un
 * faux générateur — sans device ni H3 — : la résolution tap → cellule et l'état du panneau.
 */
class TileInspectionViewModelTest : StringSpec({
    "ouvre le panneau avec le contenu de la tuile tapée" {
        val grid = FakeInspectionGrid()
        val deposits = listOf(ElementDeposit(Element.CENDRITE, richness = 0.7, ratePerHour = 42))
        val vm = TileInspectionViewModel(grid, contentOf = { TileContent(deposits) }, currentTile = NO_CURRENT)

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.deposits shouldBe deposits
    }

    "résout la cellule sous le point tapé via la grille avant d'en lire le contenu" {
        val grid = FakeInspectionGrid()
        var inspectedCell: Long? = null
        val vm = TileInspectionViewModel(
            grid,
            contentOf = { cell ->
                inspectedCell = cell
                TileContent(emptyList())
            },
            currentTile = NO_CURRENT,
        )

        // La fausse grille mappe la latitude tronquée sur la cellule : 48.0 → 48.
        vm.inspectAt(LatLng(48.0, 2.0))

        inspectedCell shouldBe 48L
    }

    "marque la tuile inspectée comme courante quand c'est celle du joueur" {
        val grid = FakeInspectionGrid()
        val vm =
            TileInspectionViewModel(grid, contentOf = { TileContent(emptyList()) }, currentTile = MutableStateFlow(48L))

        vm.inspectAt(LatLng(48.0, 2.0))
        vm.inspection.value?.isCurrent?.shouldBeTrue()

        vm.inspectAt(LatLng(49.0, 2.0))
        vm.inspection.value?.isCurrent?.shouldBeFalse()
    }

    "expose un état vide explicite pour une tuile sans gisement" {
        val grid = FakeInspectionGrid()
        val vm = TileInspectionViewModel(grid, contentOf = { TileContent(emptyList()) }, currentTile = NO_CURRENT)

        vm.inspectAt(LatLng(48.0, 2.0))

        vm.inspection.value?.isEmpty?.shouldBeTrue()
    }

    "donne un contenu identique pour deux inspections de la même tuile" {
        val grid = FakeInspectionGrid()
        // Générateur déterministe : le contenu ne dépend que de la cellule.
        val vm = TileInspectionViewModel(
            grid,
            contentOf = { cell -> TileContent(listOf(ElementDeposit(Element.GIVRELIN, cell / 100.0, cell.toInt()))) },
            currentTile = NO_CURRENT,
        )

        vm.inspectAt(LatLng(48.0, 2.0))
        val first = vm.inspection.value
        vm.inspectAt(LatLng(48.0, 2.0))
        val second = vm.inspection.value

        second shouldBe first
    }

    "referme le panneau au dismiss" {
        val grid = FakeInspectionGrid()
        val vm = TileInspectionViewModel(grid, contentOf = { TileContent(emptyList()) }, currentTile = NO_CURRENT)
        vm.inspectAt(LatLng(48.0, 2.0))

        vm.dismiss()

        vm.inspection.value.shouldBeNull()
    }
})

/** Aucune tuile courante connue (pas encore de fix GPS) : isole les cas qui ne portent pas dessus. */
private val NO_CURRENT = MutableStateFlow<Long?>(null)

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
