package com.hexa.player

import com.hexa.config.Element
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Verrouille la convention **unique** de sérialisation enum ↔ clé de champ Firestore partagée par
 * tous les mappers (cf. [PlayerDocumentMapper], [BuildingDocumentMapper]) : encodage en minuscules,
 * décodage strictement symétrique, et tolérance aux clés inconnues. Un changement de convention
 * casserait ces tests plutôt que le schéma persisté en silence.
 */
class FirestoreEnumCodecTest : StringSpec({
    "fieldKey encode une valeur d'enum par son nom en minuscules" {
        Element.CENDRITE.fieldKey shouldBe "cendrite"
        PlacedBuildingType.BASE.fieldKey shouldBe "base"
        BuildingType.EXTRACTEUR.fieldKey shouldBe "extracteur"
    }

    "toEnumOrNull relit une clé connue vers sa valeur d'enum (inverse de fieldKey)" {
        "base".toEnumOrNull<PlacedBuildingType>() shouldBe PlacedBuildingType.BASE
        "cendrite".toEnumOrNull<Element>() shouldBe Element.CENDRITE
    }

    "chaque valeur d'enum fait l'aller-retour fieldKey → toEnumOrNull sans perte" {
        PlacedBuildingType.entries.forEach { it.fieldKey.toEnumOrNull<PlacedBuildingType>() shouldBe it }
        Element.entries.forEach { it.fieldKey.toEnumOrNull<Element>() shouldBe it }
    }

    "toEnumOrNull renvoie null pour une clé inconnue plutôt que de lever" {
        "centrale_a_fusion".toEnumOrNull<PlacedBuildingType>() shouldBe null
    }

    "toEnumOrNull est strictement symétrique : une clé en casse mixte ne décode pas" {
        "BASE".toEnumOrNull<PlacedBuildingType>() shouldBe null
    }
})
