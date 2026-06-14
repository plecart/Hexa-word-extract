package com.hexa.player

import com.google.firebase.Timestamp
import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Verrouille le **contrat de schéma** Firestore : noms de champs, libellés des compteurs par élément
 * et par bâtiment, aller-retour sans perte (nanosecondes comprises) et complétion à zéro des
 * compteurs absents. Un renommage d'enum casserait ce test plutôt que le schéma persisté en silence.
 */
class PlayerDocumentMapperTest : StringSpec({
    "toDocument écrit les champs contractuels et les compteurs libellés par élément" {
        val player = Player.newPlayer(Instant.parse("2026-06-14T10:15:30Z"))

        val doc = PlayerDocumentMapper.toDocument(player)

        doc[PlayerDocumentMapper.FIELD_BASE_CELL] shouldBe null
        doc[PlayerDocumentMapper.FIELD_INVENTORY] shouldBe
            mapOf("cendrite" to 250L, "givrelin" to 100L, "lithoseve" to 0L, "echofer" to 0L, "nyctite" to 0L)
        doc[PlayerDocumentMapper.FIELD_BUILT_BUILDINGS] shouldBe mapOf("extracteur" to 0L)
        (doc[PlayerDocumentMapper.FIELD_CREATED_AT] as Timestamp).seconds shouldBe player.createdAt.epochSecond
    }

    "un document fait l'aller-retour sans perte (nanosecondes et base posée comprises)" {
        val player =
            Player.newPlayer(Instant.parse("2026-06-14T10:15:30.123456789Z"))
                .copy(baseCell = "8a1fb46622dffff")

        PlayerDocumentMapper.fromDocument(PlayerDocumentMapper.toDocument(player)) shouldBe player
    }

    "fromDocument complète les compteurs absents à zéro" {
        val doc =
            mapOf(
                PlayerDocumentMapper.FIELD_CREATED_AT to Timestamp(0, 0),
                PlayerDocumentMapper.FIELD_BASE_CELL to null,
                PlayerDocumentMapper.FIELD_INVENTORY to mapOf("cendrite" to 5L),
                PlayerDocumentMapper.FIELD_BUILT_BUILDINGS to emptyMap<String, Any?>(),
            )

        val player = PlayerDocumentMapper.fromDocument(doc)

        player.inventory[Element.CENDRITE] shouldBe 5L
        player.inventory[Element.NYCTITE] shouldBe 0L
        player.builtBuildings shouldBe mapOf(BuildingType.EXTRACTEUR to 0)
    }
})
