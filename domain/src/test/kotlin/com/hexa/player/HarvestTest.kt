package com.hexa.player

import com.hexa.config.Element
import com.hexa.world.ElementDeposit
import com.hexa.world.TileContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Module **pur** de calcul de la récolte paresseuse (cf. PRD #5, user stories 12-13) : à horloge et
 * générateur de contenu injectés, sans aucune I/O. Vérifie le contrat « régler les comptes jusqu'à
 * maintenant » — gain = vitesse × temps écoulé, [PlacedBuilding.lastCollectedAt] avancé à `now` quand
 * un crédit a lieu — sur des durées de quelques secondes à plusieurs semaines, en multi-bâtiments et
 * multi-éléments, et sur les cas à gain nul (tuile sans gisement, gisement à vitesse nulle, tick trop
 * court pour produire une unité entière).
 */
class HarvestTest : StringSpec({
    val now = Instant.parse("2026-06-20T12:00:00Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    /** Bâtiment dont la dernière récolte remonte à [elapsed] avant `now`, posé sur la tuile [cell]. */
    fun buildingPlacedSince(elapsed: Duration, cell: String = "tuile"): PlacedBuilding {
        val at = now.minus(elapsed)
        return PlacedBuilding(cell = cell, type = PlacedBuildingType.BASE, placedAt = at, lastCollectedAt = at)
    }

    /** Générateur de contenu injecté : associe chaque tuile à ses gisements (vitesses en u/h). */
    fun worldOf(vararg tiles: Pair<String, List<ElementDeposit>>): (String) -> TileContent {
        val byCell = tiles.toMap()
        return { cell -> TileContent(byCell[cell].orEmpty()) }
    }

    fun deposit(element: Element, ratePerHour: Int) = ElementDeposit(element, richness = 1.0, ratePerHour = ratePerHour)

    "une heure pile à 60 u/h crédite 60 unités et avance lastCollectedAt exactement à now" {
        val building = buildingPlacedSince(Duration.ofHours(1))
        val calculator = HarvestCalculator(clock, worldOf("tuile" to listOf(deposit(Element.CENDRITE, 60))))

        val result = calculator.collect(listOf(building))

        result.gains shouldContainExactly mapOf(Element.CENDRITE to 60L)
        result.collected.single().lastCollectedAt shouldBe now
    }

    "le gain suit la durée écoulée : 30 minutes à 60 u/h créditent 30 unités" {
        val building = buildingPlacedSince(Duration.ofMinutes(30))
        val calculator = HarvestCalculator(clock, worldOf("tuile" to listOf(deposit(Element.CENDRITE, 60))))

        calculator.collect(listOf(building)).gains shouldContainExactly mapOf(Element.CENDRITE to 30L)
    }

    "le gain hors ligne tient sur de longues durées : une semaine à 14 u/h crédite 2352 unités" {
        val building = buildingPlacedSince(Duration.ofDays(7))
        val calculator = HarvestCalculator(clock, worldOf("tuile" to listOf(deposit(Element.LITHOSEVE, 14))))

        calculator.collect(listOf(building)).gains shouldContainExactly mapOf(Element.LITHOSEVE to 2352L)
    }

    "multi-éléments : chaque gisement de la tuile est crédité indépendamment" {
        val building = buildingPlacedSince(Duration.ofHours(2))
        val calculator = HarvestCalculator(
            clock,
            worldOf("tuile" to listOf(deposit(Element.CENDRITE, 60), deposit(Element.GIVRELIN, 30))),
        )

        calculator.collect(listOf(building)).gains shouldContainExactly
            mapOf(Element.CENDRITE to 120L, Element.GIVRELIN to 60L)
    }

    "multi-bâtiments : les gains s'additionnent et chaque lastCollectedAt est avancé à now" {
        val mine = buildingPlacedSince(Duration.ofHours(1), cell = "mine")
        val base = buildingPlacedSince(Duration.ofHours(3), cell = "base")
        val calculator = HarvestCalculator(
            clock,
            worldOf(
                "mine" to listOf(deposit(Element.CENDRITE, 60)),
                "base" to listOf(deposit(Element.CENDRITE, 30)),
            ),
        )

        val result = calculator.collect(listOf(mine, base))

        result.gains shouldContainExactly mapOf(Element.CENDRITE to 150L) // 60×1h + 30×3h
        result.collected.map { it.lastCollectedAt } shouldContainExactlyInAnyOrder listOf(now, now)
    }

    "tuile sans gisement : gain nul et lastCollectedAt laissé en place (rien à régler)" {
        val building = buildingPlacedSince(Duration.ofDays(1))
        val calculator = HarvestCalculator(clock, worldOf()) // aucune tuile → contenu vide

        val result = calculator.collect(listOf(building))

        result.gains shouldBe emptyMap()
        result.collected shouldBe emptyList()
    }

    "gisement présent mais vitesse nulle (élément rare arrondi à 0 u/h) : aucun crédit" {
        val building = buildingPlacedSince(Duration.ofDays(7))
        val calculator = HarvestCalculator(clock, worldOf("tuile" to listOf(deposit(Element.NYCTITE, 0))))

        val result = calculator.collect(listOf(building))

        result.gains shouldBe emptyMap()
        result.collected shouldBe emptyList()
    }

    "tick trop court pour une unité entière : aucun crédit, lastCollectedAt préservé pour cumuler" {
        val building = buildingPlacedSince(Duration.ofSeconds(30)) // 60 u/h × 30 s = 0,5 → 0
        val calculator = HarvestCalculator(clock, worldOf("tuile" to listOf(deposit(Element.CENDRITE, 60))))

        val result = calculator.collect(listOf(building))

        result.gains shouldBe emptyMap()
        result.collected shouldBe emptyList()
    }
})
