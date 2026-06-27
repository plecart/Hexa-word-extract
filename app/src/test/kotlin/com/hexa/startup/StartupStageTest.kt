package com.hexa.startup

import com.hexa.core.geo.LatLng
import com.hexa.player.Inventory
import com.hexa.player.PlayerUiState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Sélecteur d'état de la **machine à états de démarrage** ([startupStage]) : décide, à partir du
 * signal de premier fix GPS et de l'état du compte, l'écran à afficher. Le **premier fix prime** —
 * tant qu'il manque, on reste sur l'écran de chargement quel que soit le compte (jamais la carte,
 * jamais la pose de base). Une fois la position connue, la pose de base s'affiche tant que la base
 * n'est pas posée, sinon la carte de jeu (la gestion de la permission vit, elle, dans
 * `LocationPermissionGate`, en amont).
 */
class StartupStageTest : StringSpec({
    val position = LatLng(48.8566, 2.3522)
    fun ready(baseCell: String?) = PlayerUiState.Ready("uid", Inventory.EMPTY, emptyMap(), baseCell)

    "sans premier fix, l'écran de chargement prime même compte prêt et base posée" {
        startupStage(firstFix = null, playerState = ready(baseCell = "8a1f")) shouldBe StartupStage.LOADING
    }

    "sans premier fix pendant l'amorçage du compte, écran de chargement" {
        startupStage(firstFix = null, playerState = PlayerUiState.Loading) shouldBe StartupStage.LOADING
    }

    "premier fix connu et base non posée affiche la pose de base" {
        startupStage(firstFix = position, playerState = ready(baseCell = null)) shouldBe StartupStage.FIRST_LAUNCH
    }

    "premier fix connu et base posée affiche la carte de jeu" {
        startupStage(firstFix = position, playerState = ready(baseCell = "8a1f")) shouldBe StartupStage.GAME
    }

    "premier fix connu pendant l'amorçage du compte affiche la carte de jeu" {
        startupStage(firstFix = position, playerState = PlayerUiState.Loading) shouldBe StartupStage.GAME
    }

    "premier fix connu après un échec d'amorçage affiche la carte de jeu" {
        startupStage(firstFix = position, playerState = PlayerUiState.Failed) shouldBe StartupStage.GAME
    }
})
