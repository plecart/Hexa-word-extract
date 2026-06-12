# Hexa

Prototype Android d'un jeu de collecte de ressources géolocalisé : le joueur se déplace dans le
monde réel, prospecte des tuiles hexagonales (grille H3) générées procéduralement, et pose des
extracteurs qui produisent en continu — même application fermée.

**Statut : MVP technique** — un prototype de validation, pas un jeu équilibré. Les cinq questions
auxquelles il doit répondre (carte fluide, fiabilité GPS, rendu 3D ancré, génération procédurale
déterministe, persistance + récolte hors ligne) sont détaillées dans les spécifications.

## Documents de référence

- [Spécifications fonctionnelles](Document/Hexa_01_Specifications_fonctionnelles.md) — périmètre,
  architecture, fonctionnalités F1–F9, écrans, risques.
- [Game design et équilibrage](Document/Hexa_02_Game_design_et_equilibrage.md) — boucle de jeu,
  algorithme de génération (bruit simplex 3D sur la sphère), économie, constantes de configuration.

## Stack

| Brique | Choix |
|---|---|
| Plateforme | Android natif, Kotlin + Jetpack Compose |
| Carte | Mapbox Maps SDK for Android v11 (extension Compose) |
| 3D placeholders | FillExtrusionLayer (cubes extrudés) |
| Grille | H3 (`com.uber:h3`), résolution 11 |
| GPS | FusedLocationProviderClient |
| Backend | Firebase Auth (anonyme) + Cloud Firestore |
| Génération | Bruit simplex 3D en Kotlin, déterministe |

## Pipeline de développement

Le projet suit une pipeline pilotée par les issues (PRD → issues « tranches verticales » → triage →
agents → PR → QA). Voir [CONTRIBUTING.md](CONTRIBUTING.md) et les règles permanentes dans
[.claude/rules/](.claude/rules/). Les skills de la pipeline sont déployés dans
[.claude/skills/](.claude/skills/) ; leur source (seed `moon-pipeline-dev`) est conservée dans
[skills/](skills/) et [template/](template/).
