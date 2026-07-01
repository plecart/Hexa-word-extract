package com.hexa.map

import com.hexa.config.GameConfig
import com.hexa.core.geo.LatLng
import com.hexa.world.TileCenterLocator
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [H3Grid] est l'adaptateur de production vers la bibliothèque native H3 d'Uber : il résout la
 * cellule sous une position, le disque de cellules autour d'elle et le contour à dessiner, et sert
 * de [TileCenterLocator] au générateur de monde. On vérifie ces conversions sur une cellule réelle —
 * H3 tourne en test JVM, comme côté `:domain`.
 */
class HexGridTest : StringSpec({
    val grid = H3Grid()
    val paris = LatLng(48.8566, 2.3522)

    "résout la cellule sous une position, dont le centre retombe dans la même cellule" {
        val cell = grid.cellAt(paris)
        grid.cellAt(grid.centerOf(cell)) shouldBe cell
    }

    "un disque de k anneaux contient 1 + 3k(k+1) cellules, centre inclus" {
        val center = grid.cellAt(paris)
        // Bornes du jeu : 2 anneaux → 19 cellules, 4 anneaux → 61 cellules.
        grid.disk(center, rings = 2) shouldHaveSize 19
        grid.disk(center, rings = 4) shouldHaveSize 61
        grid.disk(center, rings = 2) shouldContain center
    }

    "le contour d'une cellule hexagonale a six sommets" {
        val cell = grid.cellAt(paris)
        grid.outline(cell) shouldHaveSize 6
    }

    "la distance en anneaux vaut 0 pour la cellule elle-même, 1 pour une voisine, k au k-ième anneau" {
        val center = grid.cellAt(paris)
        grid.gridDistance(center, center) shouldBe 0
        // Toute cellule du disque de 1 anneau autre que le centre est une voisine immédiate (distance 1).
        grid.disk(center, rings = 1).filter { it != center }.forAll { grid.gridDistance(center, it) shouldBe 1 }
        // Une cellule strictement au 2ᵉ anneau (dans le disque de 2 mais pas celui de 1) est à distance 2.
        val ringTwo = grid.disk(center, rings = 2).first { it !in grid.disk(center, rings = 1) }
        grid.gridDistance(center, ringTwo) shouldBe 2
        // La distance est symétrique.
        grid.gridDistance(ringTwo, center) shouldBe 2
    }

    "convertit une cellule en son index H3 textuel canonique (hexadécimal), stable et distinct" {
        val cell = grid.cellAt(paris)
        val other = grid.cellAt(LatLng(48.86, 2.36))
        // Forme canonique H3 = hexadécimal de l'index ; c'est le contrat de `Player.baseCell` / l'ID de
        // document `buildings/{h3Index}`.
        grid.toH3String(cell) shouldBe java.lang.Long.toHexString(cell)
        grid.toH3String(cell) shouldBe grid.toH3String(cell)
        grid.toH3String(cell) shouldNotBe grid.toH3String(other)
    }

    "reconstruit l'index H3 numérique depuis sa forme textuelle (inverse de toH3String)" {
        val cell = grid.cellAt(paris)
        // Le pont String → Long que consomme le générateur de monde lors de la récolte (#25).
        grid.toH3Index(grid.toH3String(cell)) shouldBe cell
    }

    "expose le port TileCenterLocator pour le générateur de monde" {
        grid.shouldBeInstanceOf<TileCenterLocator>()
    }

    "résout la cellule à la résolution centrale du jeu par défaut" {
        grid.cellAt(paris) shouldBe H3Grid(resolution = GameConfig.H3_RESOLUTION).cellAt(paris)
        // À une autre résolution, la même position tombe dans une cellule d'index différent : la
        // résolution est bien un réglage, lu par défaut depuis la configuration centrale.
        grid.cellAt(paris) shouldNotBe H3Grid(resolution = GameConfig.H3_RESOLUTION - 1).cellAt(paris)
    }
})
