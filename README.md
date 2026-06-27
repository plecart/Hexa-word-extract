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
| Rendu 3D — bâtiments | ModelLayer (modèles `.glb`) |
| Rendu 3D — avatar | ModelLayer (modèle `.glb`) |
| Grille | H3 (`com.uber:h3`), résolution 10 |
| GPS | FusedLocationProviderClient |
| Backend | Firebase Auth (anonyme) + Cloud Firestore |
| Génération | Bruit simplex 3D en Kotlin, déterministe |

## Build & lancement

### Prérequis

- **JDK 17** (le wrapper Gradle s'attend à `JAVA_HOME` pointant sur un JDK 17).
- **Android SDK** avec la plateforme `android-35`. Renseigner son emplacement dans un fichier
  `local.properties` (non versionné) à la racine :

  ```properties
  sdk.dir=/chemin/vers/Android/Sdk
  ```

- **Tokens Mapbox** — la carte passe par le Mapbox Maps SDK, qui réclame **deux** tokens (créés sur
  [account.mapbox.com/access-tokens](https://account.mapbox.com/access-tokens)), placés dans des
  fichiers **non versionnés** :

  | Token | Rôle | Fichier (non versionné) | Scope |
  |---|---|---|---|
  | Public (`pk.…`) | chargé au runtime par l'app | `local.properties` (racine du dépôt) | scopes publics par défaut |
  | Secret (`sk.…`) | télécharge le SDK Mapbox au build | `~/.gradle/gradle.properties` (dossier utilisateur, hors dépôt) | `DOWNLOADS:READ` |

  ```properties
  # local.properties (à la racine du dépôt — ignoré par git)
  MAPBOX_PUBLIC_TOKEN=pk.votre_token_public

  # ~/.gradle/gradle.properties (dossier utilisateur global — hors dépôt)
  MAPBOX_DOWNLOADS_TOKEN=sk.votre_token_secret
  ```

  ⚠️ Un token n'apparaît **jamais** dans un fichier versionné : `local.properties` est dans le
  `.gitignore` et le token de téléchargement vit hors du dépôt. Mapbox **ne propose aucun plafond de
  dépense** automatique ; le palier gratuit du Maps SDK Mobile (facturé à l'utilisateur actif
  mensuel) couvre très largement le développement, et la rotation du token sur le compte Mapbox sert
  de coupe-circuit en cas de dérive.

- **Style de carte** — un style monochrome sobre dérivé de **Mapbox Standard**, pensé pour que
  seuls l'avatar et les bâtiments posés par le joueur ressortent : sol plat, pas de bâtiment réel,
  aucun texte.

  - **Style URL** : `mapbox://styles/pbernier/cmqcpqcwy001m01s4eumxepf8`
  - **Recette** (reproductible dans Mapbox Studio à partir d'un import *Mapbox Standard*) : thème
    *Monochrome*, light preset *Day* ; **tous les labels coupés** (lieux, POI, routes, transit) et
    **frontières administratives** masquées ; **tous les objets 3D coupés** (bâtiments, arbres,
    landmarks) ; rues et chemins piétons conservés ; eau et espaces verts conservés comme repères.
  - Repli si les empreintes de bâtiments plates de Standard gênent sous la caméra inclinée :
    repartir d'un style classique dépourvu de couche `building` (suppression garantie).

- **Firebase** — le backend (Auth anonyme + Cloud Firestore) repose sur un fichier de configuration
  **non versionné** (dépôt public). Mise en place pour un nouveau poste :

  1. Dans la [console Firebase](https://console.firebase.google.com), ouvrir le projet `hexa-word-extract`
     (ou en recréer un, tier Spark) → *Paramètres du projet* → application Android `com.hexa`
     → télécharger **`google-services.json`** et le placer dans **`app/google-services.json`**
     (ignoré par git ; le plugin `google-services` le consomme au build).
  2. Activer **Authentication → Anonyme** et provisionner **Cloud Firestore**.
  3. Publier les règles de sécurité : coller [`firestore.rules`](firestore.rules) dans la console
     (onglet *Rules*) ou `firebase deploy --only firestore:rules`. Elles limitent chaque joueur à
     son document `players/{uid}` et ses sous-collections.

  ⚠️ `google-services.json` n'est **jamais** versionné. En CI, il est reconstitué depuis le secret
  de dépôt **`GOOGLE_SERVICES_JSON`** (contenu encodé en base64).

- **Grille H3 sur Android** — `com.uber:h3` embarque ses binaires natifs comme ressources du
  classpath (par plateforme), qu'AGP n'empaquette pas. Le build les **extrait vers les `jniLibs`** de
  l'APK (tâche `extractH3Natives` dans [`app/build.gradle.kts`](app/build.gradle.kts)) et l'app les
  charge via `H3Core.newSystemInstance()`. ⚠️ La lib ne fournit de `.so` que pour **arm/arm64** : la
  grille hexagonale ne fonctionne donc **pas sur un émulateur x86/x86_64** (`UnsatisfiedLinkError`) —
  valider sur un **appareil ARM réel** ou un émulateur arm64.

Aucune installation de Gradle n'est nécessaire : le wrapper (`./gradlew`) télécharge la version
attendue.

### Commandes

| But | Commande |
|---|---|
| Compiler l'APK de debug | `./gradlew assembleDebug` |
| Installer sur un appareil/émulateur branché | `./gradlew installDebug` |
| Chaîne qualité (lint + format + tests) | `./gradlew ktlintCheck lintDebug testDebugUnitTest :core:test :domain:test` |
| Formater le code automatiquement | `./gradlew ktlintFormat` |
| Mesurer la distribution du monde (rapport mesuré vs cibles + seuils proposés) | `./gradlew :domain:worldDistributionReport` |

L'APK généré se trouve sous `app/build/outputs/apk/debug/`. Au lancement, l'application amorce
silencieusement le compte joueur puis affiche la carte plein écran ; un bouton flottant ouvre la
page d'inventaire à deux onglets (« Ressources », « Bâtiments »). L'onglet Ressources liste les cinq
éléments avec leur quantité, mise à jour en temps réel depuis le document joueur ; l'onglet Bâtiments
montre le stock d'extracteurs prêts à poser et leur recette de craft, avec un bouton « Construire ».

La même chaîne qualité s'exécute sur chaque PR via GitHub Actions
([.github/workflows/ci.yml](.github/workflows/ci.yml)).

## Architecture des modules

Le projet est découpé en modules Gradle pour rendre **structurelle** (vérifiée par le build, pas
seulement par discipline) la frontière entre le code Android et le cœur logique réutilisable :

| Module | Type | Rôle | Dépend de |
|---|---|---|---|
| `:app` | Android application | Point d'entrée Android, UI Compose, câblage ; intégration Firebase (Auth anonyme + Firestore avec cache offline) derrière les ports du domaine | `:domain`, `:location` |
| `:domain` | Kotlin pur | Configuration d'équilibrage ([`GameConfig`](domain/src/main/kotlin/com/hexa/config/GameConfig.kt), [`Element`](domain/src/main/kotlin/com/hexa/config/Element.kt)), générateur procédural du monde ([`WorldGenerator`](domain/src/main/kotlin/com/hexa/world/WorldGenerator.kt) : index H3 → contenu de tuile) et compte joueur — document, inventaire et amorçage idempotent ([`EnsurePlayerUseCase`](domain/src/main/kotlin/com/hexa/player/EnsurePlayerUseCase.kt)) derrière les ports [`PlayerRepository`](domain/src/main/kotlin/com/hexa/player/PlayerRepository.kt) / [`AuthGateway`](domain/src/main/kotlin/com/hexa/player/AuthGateway.kt) | `:core` |
| `:location` | Kotlin pur | Poursuite de caméra à la 3ᵉ personne sans dépendance Mapbox : contrôleur de caméra ([`ChaseCameraController`](location/src/main/kotlin/com/hexa/location/ChaseCameraController.kt)), lissage de cap circulaire ([`HeadingSmoother`](location/src/main/kotlin/com/hexa/location/HeadingSmoother.kt)), sources de position derrière interface ([`PositionSource`](location/src/main/kotlin/com/hexa/location/PositionSource.kt)) | `:core` |
| `:core` | Kotlin pur | Utilitaires génériques sans sémantique métier : géométrie ([`UnitSphere`](core/src/main/kotlin/com/hexa/core/geo/UnitSphere.kt)), bruit procédural | — |

Règle de dépendances : `:app` dépend des modules purs (`:domain`, `:location`), eux-mêmes sur
`:core` ; jamais l'inverse. Les modules Kotlin purs n'ont **aucune dépendance Android** — le SDK
Android n'est pas sur leur classpath, et `:location` n'a pas non plus le SDK Mapbox : la logique de
poursuite reste testable hors device. Le générateur
procédural du monde vit dans `:domain` et consomme `:core` ; la bibliothèque H3 (native) reste hors
de `:domain`, derrière le port [`TileCenterLocator`](domain/src/main/kotlin/com/hexa/world/TileCenterLocator.kt),
si bien que `:domain` reste partageable plus tard avec un serveur. L'**unique** intégration H3 de
production vit dans `:app` ([`HexGrid`](app/src/main/java/com/hexa/map/HexGrid.kt) / `H3Grid`) : elle
dessine la grille hexagonale autour du joueur **et** implémente `TileCenterLocator` pour le
générateur, évitant une seconde intégration native.

La configuration commune des modules Kotlin purs (toolchain JVM 17, ktlint, Kotest) est définie
**une seule fois** dans le plugin de convention `hexa.kotlin-pure-library` (build composite
`build-logic/`) ; chaque module pur l'applique en une ligne.

Les tests d'UI Compose de `:app` tournent **en JVM via Robolectric**, dans `src/test` (harnais
`ui-test-junit4`), sans émulateur — l'instrumenté (`androidTest`) est volontairement écarté, fragile
sur ce projet. JUnit 4 (Compose UI-test) et JUnit 5 (Kotest) cohabitent sous la même plateforme
JUnit grâce à `junit-vintage-engine`. Ces tests pilotent l'arbre sémantique Compose, sans Mapbox/GPS.

## Pipeline de développement

Le projet suit une pipeline pilotée par les issues (PRD → issues « tranches verticales » → triage →
agents → PR → QA). Voir [CONTRIBUTING.md](CONTRIBUTING.md) et les règles permanentes dans
[.claude/rules/](.claude/rules/). Les skills de la pipeline vivent dans
[.claude/skills/](.claude/skills/) (source : seed `moon-pipeline-dev`).
