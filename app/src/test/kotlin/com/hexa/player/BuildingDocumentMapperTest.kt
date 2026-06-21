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
})
