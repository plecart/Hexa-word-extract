package com.hexa.ui.theme

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan

/**
 * Verrouille l'ombrage du bloc placeholder ([cubeFacesOf]) : c'est lui qui donne au cube son **relief
 * « 3D »** à partir de la seule couleur d'identité. On contractualise l'ordre de luminosité (dessus
 * éclairci, côté droit assombri) et la distinction des trois faces — sans ces invariants, le cube
 * s'aplatit en une pastille unie et perd son rôle de placeholder d'objet 3D.
 */
class CubeFacesTest : StringSpec({
    // Luminance perceptuelle (Rec. 601) : pondère les canaux pour ordonner « plus clair / plus sombre ».
    fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

    val base = Color(0xFF808080)
    val faces = cubeFacesOf(base)

    "le dessus est plus clair que la base" {
        faces.top.luminance() shouldBeGreaterThan base.luminance()
    }

    "le côté droit est plus sombre que la base" {
        faces.right.luminance() shouldBeLessThan base.luminance()
    }

    "les trois faces sont distinctes" {
        setOf(faces.top, faces.left, faces.right) shouldHaveSize 3
    }
})
