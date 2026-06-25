package com.hexa.player

import com.google.firebase.Timestamp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Verrouille le **contrat de schéma** du document `players/{uid}/buildings/{h3Index}` (cf. spec F5) :
 * libellé contractuel du type, et les deux bornes temporelles (`placedAt`, `lastCollectedAt`)
 * écrites distinctement avec leurs nanosecondes. Un renommage d'enum casserait ce test plutôt que
 * le schéma persisté en silence.
 */
class BuildingDocumentMapperTest : StringSpec({
    "toDocument écrit le type contractuel et les deux bornes temporelles distinctes" {
        val placedAt = Instant.parse("2026-06-20T09:00:00.123456789Z")
        val lastCollectedAt = Instant.parse("2026-06-21T10:00:00Z")
        val building = PlacedBuilding("8a1fb46622dffff", PlacedBuildingType.BASE, placedAt, lastCollectedAt)

        val doc = BuildingDocumentMapper.toDocument(building)

        doc[BuildingDocumentMapper.FIELD_TYPE] shouldBe "base"
        doc[BuildingDocumentMapper.FIELD_PLACED_AT] shouldBe Timestamp(placedAt.epochSecond, placedAt.nano)
        doc[BuildingDocumentMapper.FIELD_LAST_COLLECTED_AT] shouldBe
            Timestamp(lastCollectedAt.epochSecond, lastCollectedAt.nano)
    }

    "fromDocument relit le type, l'index H3 (id du doc) et les bornes temporelles avec leurs nanos" {
        val cell = "8a1fb46622dffff"
        val placedAt = Instant.parse("2026-06-20T09:00:00.123456789Z")
        val lastCollectedAt = Instant.parse("2026-06-21T10:00:00Z")
        val data = mapOf<String, Any?>(
            BuildingDocumentMapper.FIELD_TYPE to "base",
            BuildingDocumentMapper.FIELD_PLACED_AT to Timestamp(placedAt.epochSecond, placedAt.nano),
            BuildingDocumentMapper.FIELD_LAST_COLLECTED_AT to
                Timestamp(lastCollectedAt.epochSecond, lastCollectedAt.nano),
        )

        BuildingDocumentMapper.fromDocument(cell, data) shouldBe
            PlacedBuilding(cell, PlacedBuildingType.BASE, placedAt, lastCollectedAt)
    }

    "toDocument puis fromDocument restitue le bâtiment d'origine (aller-retour)" {
        val building = PlacedBuilding(
            "8a1fb46622dffff",
            PlacedBuildingType.BASE,
            Instant.parse("2026-06-20T09:00:00.123456789Z"),
            Instant.parse("2026-06-21T10:00:00.987654321Z"),
        )

        val document = BuildingDocumentMapper.toDocument(building)
        BuildingDocumentMapper.fromDocument(building.cell, document) shouldBe building
    }

    "un extracteur posé écrit le type contractuel extracteur et se relit à l'identique" {
        val building = PlacedBuilding(
            "8a1fb46622dffff",
            PlacedBuildingType.EXTRACTEUR,
            Instant.parse("2026-06-22T08:30:00.123456789Z"),
            Instant.parse("2026-06-22T08:30:00.123456789Z"),
        )

        val document = BuildingDocumentMapper.toDocument(building)

        document[BuildingDocumentMapper.FIELD_TYPE] shouldBe "extracteur"
        BuildingDocumentMapper.fromDocument(building.cell, document) shouldBe building
    }

    "fromDocument écarte (null) un document au type inconnu plutôt que de lever" {
        val data = mapOf<String, Any?>(
            BuildingDocumentMapper.FIELD_TYPE to "centrale_a_fusion",
            BuildingDocumentMapper.FIELD_PLACED_AT to Timestamp(0, 0),
            BuildingDocumentMapper.FIELD_LAST_COLLECTED_AT to Timestamp(0, 0),
        )

        BuildingDocumentMapper.fromDocument("8a1fb46622dffff", data) shouldBe null
    }

    "fromDocument écarte (null) un document sans aucun champ" {
        BuildingDocumentMapper.fromDocument("8a1fb46622dffff", emptyMap()) shouldBe null
    }

    "fromDocument écarte (null) un document dont le type n'est pas une chaîne" {
        val data = mapOf<String, Any?>(
            BuildingDocumentMapper.FIELD_TYPE to 42L,
            BuildingDocumentMapper.FIELD_PLACED_AT to Timestamp(0, 0),
            BuildingDocumentMapper.FIELD_LAST_COLLECTED_AT to Timestamp(0, 0),
        )

        BuildingDocumentMapper.fromDocument("8a1fb46622dffff", data) shouldBe null
    }

    "fromDocument écarte (null) un document dont placedAt n'est pas un Timestamp" {
        val data = mapOf<String, Any?>(
            BuildingDocumentMapper.FIELD_TYPE to "base",
            BuildingDocumentMapper.FIELD_PLACED_AT to "pas-un-timestamp",
            BuildingDocumentMapper.FIELD_LAST_COLLECTED_AT to Timestamp(0, 0),
        )

        BuildingDocumentMapper.fromDocument("8a1fb46622dffff", data) shouldBe null
    }

    "fromDocument écarte (null) un document dont lastCollectedAt est absent" {
        val data = mapOf<String, Any?>(
            BuildingDocumentMapper.FIELD_TYPE to "base",
            BuildingDocumentMapper.FIELD_PLACED_AT to Timestamp(0, 0),
        )

        BuildingDocumentMapper.fromDocument("8a1fb46622dffff", data) shouldBe null
    }
})
