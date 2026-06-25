# Hexa — Spécifications fonctionnelles (MVP technique)

**Version 0.1 — juin 2026 — document vivant**

---

## 1. Objectif du MVP

Ce MVP est un **prototype de validation technique**, pas un jeu équilibré. Le gameplay est volontairement minimal. Les questions auxquelles ce prototype doit répondre sont les suivantes :

1. Peut-on afficher une carte du monde sobre avec une grille hexagonale H3 superposée, de façon fluide sur Android ?
2. Le GPS est-il assez précis et stable pour positionner un avatar et déterminer de manière fiable la tuile H3 sur laquelle se trouve le joueur ?
3. Peut-on afficher des objets 3D (modèles `.glb` pour les bâtiments, cube extrudé pour l'avatar) ancrés sur des tuiles de la carte, avec une caméra de poursuite à la troisième personne ?
4. La génération procédurale des ressources par seeding (position → contenu) fonctionne-t-elle de manière déterministe et performante, sans rien stocker ?
5. La persistance des données joueur (inventaire, bâtiments placés) et le calcul de la récolte hors ligne fonctionnent-ils correctement ?

Tout ce qui ne sert pas à répondre à ces cinq questions est hors périmètre.

## 2. Périmètre

### Inclus dans le MVP

- Application Android native (un seul OS pour ce prototype).
- Carte du monde en style sobre (rues et bâtiments à plat, monochrome).
- Avatar du joueur positionné par GPS, caméra de poursuite à la troisième personne.
- Grille hexagonale H3 affichée en surcouche autour du joueur.
- Inspection d'une tuile au tap : affichage des ressources qu'elle contient et de leur vitesse d'extraction.
- Placement d'un bâtiment **uniquement sur la tuile où se trouve physiquement le joueur** (logique Pokémon Go). Un seul bâtiment par tuile. Tout placement est permanent.
- Un seul type de bâtiment : **l'extracteur**. La **base** du joueur est elle-même un extracteur (premier bâtiment placé, non déplaçable).
- Bâtiments représentés par des modèles 3D (`.glb`) posés sur la tuile.
- Inventaire à deux onglets : « Ressources » et « Bâtiments » (avec le craft intégré à l'onglet Bâtiments).
- Récolte continue, y compris hors ligne, créditée directement dans l'inventaire. Stockage illimité, nombre d'extracteurs illimité.
- Persistance des données joueur dans une base de données distante, avec authentification anonyme (pas de SSO pour ce prototype, mais une architecture qui permettra d'en ajouter un plus tard sans migration douloureuse).

### Explicitement hors périmètre (reporté post-MVP)

Équilibrage fin du gameplay, SSO (Google/Apple), multijoueur, anti-triche (GPS spoofing, manipulation d'horloge — tous les calculs peuvent rester côté client pour ce prototype), monétisation, iOS, capacité de stockage des extracteurs, déplacement/suppression de bâtiments, autres bâtiments que l'extracteur, modèles 3D définitifs, notifications, tutoriel.

## 3. Architecture technique

### Stack retenue

| Brique | Choix | Justification |
|---|---|---|
| Plateforme | Android natif, **Kotlin + Jetpack Compose** | Pas de moteur de jeu nécessaire ; écosystème où l'assistance IA au développement est la plus efficace |
| Carte | **Mapbox Maps SDK for Android v11** (avec l'extension Compose) | Rendu 3D natif (extrusions, modèles glTF), styles personnalisables à volonté via Mapbox Studio (idéal pour le rendu « sobre »), caméra librement contrôlable (pitch, bearing, zoom), offre gratuite généreuse pour un prototype |
| Rendu 3D | Bâtiments via **ModelLayer** (modèles `.glb`) ; avatar via **FillExtrusionLayer** (un polygone extrudé = un cube) | Le passage des extrusions placeholders aux modèles `.glb` (prévu sans changer d'architecture) est réalisé pour les bâtiments ; l'avatar reste un cube extrudé en attendant son propre modèle |
| Grille | **H3** (bibliothèque Java officielle d'Uber, `com.uber:h3`) | Index unique et stable pour chaque hexagone du globe → sert directement de seed ; conversions position ↔ cellule et cellule → contour fournies |
| GPS | FusedLocationProviderClient (Google Play Services) | Standard Android, fusion GPS/Wi-Fi/réseau |
| Backend / DB | **Firebase** : Auth (mode anonyme) + Cloud Firestore | Gratuit au tier Spark pour un prototype, SDK Android natif, passage de l'auth anonyme vers Google/Apple SSO prévu par Firebase (account linking), synchronisation offline incluse |
| Génération procédurale | Bruit simplex 3D implémenté en Kotlin (déterministe, partagé plus tard avec le serveur) | Voir le document de game design |

À noter : aucune information de terrain réel (eau, parcs, altitude) n'est exploitée pour la distribution des ressources — H3 n'embarque pas ces données, donc conformément à la décision prise, on l'ignore. La distribution est purement procédurale (bruit + position).

### Résolution H3

La cible initiale était une tuile d'environ 25 m. Correspondances H3 (valeurs moyennes, les hexagones H3 varient légèrement en taille) :

| Résolution | Arête moyenne | Largeur approximative | Surface moyenne |
|---|---|---|---|
| 10 | ~66 m | ~130 m | ~15 000 m² |
| **11 (retenue)** | **~25 m** | **~50 m** | **~2 150 m²** |
| 12 | ~9 m | ~19 m | ~310 m² |

**Choix provisoire : résolution 11** (arête ≈ 25 m, soit un hexagone d'environ 50 m de large — proche de l'intention, et une taille où la précision GPS grand public, typiquement 5 à 15 m, reste fiable pour déterminer la tuile courante). La résolution 12 collerait mieux à « 25 m de large » mais la précision GPS deviendrait un vrai problème (le joueur « sauterait » de tuile en tuile). À valider sur le terrain : c'est précisément l'un des objectifs du prototype. La résolution sera une constante de configuration unique dans le code.

### Schéma de données (Firestore)

```
players/{playerId}                  // playerId = uid Firebase anonyme
  createdAt: timestamp
  baseCell: string                  // index H3 de la base (null tant que non placée)
  inventory: {                      // compteurs cumulés
    cendrite: number,
    givrelin: number,
    lithoseve: number,
    echofer: number,
    nyctite: number
  }
  builtBuildings: {                 // bâtiments craftés non encore posés
    extracteur: number
  }

players/{playerId}/buildings/{h3Index}   // 1 doc par bâtiment placé, ID = index H3
  type: "base" | "extracteur"
  placedAt: timestamp
  lastCollectedAt: timestamp        // borne de départ du prochain calcul de récolte
```

Le contenu des tuiles n'est **jamais stocké** : il est recalculé à la volée depuis l'index H3 (voir document de game design). Utiliser l'index H3 comme ID de document garantit gratuitement la règle « un bâtiment par tuile ».

## 4. Fonctionnalités détaillées

### F1 — Carte et caméra

La vue principale affiche la carte Mapbox dans un style personnalisé monochrome : fond uni, tracé des rues, empreintes des bâtiments à plat, pas de labels superflus. La caméra est en poursuite à la troisième personne : centrée sur l'avatar, inclinée (pitch ~55–65°), légèrement zoomée (de quoi voir ~150–300 m autour du joueur). Le cap de la caméra suit l'orientation de l'appareil (boussole) avec lissage. Un pincement permet d'ajuster le zoom dans une plage bornée ; un bouton recentre sur l'avatar si l'utilisateur a déplacé la carte.

### F2 — Avatar

Un marqueur 3D (cube placeholder d'une couleur distincte) positionné sur la position GPS lissée du joueur. La position brute du GPS est filtrée (lissage exponentiel + rejet des points aberrants de faible précision) pour éviter les tremblements et les sauts de tuile intempestifs.

### F3 — Grille hexagonale

Les hexagones H3 (résolution 11) sont dessinés en surcouche fine au-dessus de la carte, uniquement dans la zone visible autour du joueur (calcul par `gridDisk` autour de la cellule courante, rayon adapté au zoom — typiquement 2 à 4 anneaux, soit 19 à 61 cellules). La tuile sur laquelle se trouve le joueur est surlignée. Les tuiles contenant un bâtiment du joueur sont visuellement distinctes.

### F4 — Inspection de tuile

Un tap sur une tuile ouvre un panneau (bottom sheet) affichant : la liste des éléments présents sur cette tuile (0 à 5), leur richesse et la vitesse d'extraction correspondante (valeurs issues de la génération procédurale, identiques à chaque consultation). Si la tuile inspectée est **celle où se trouve le joueur**, qu'elle est libre, et que l'inventaire contient au moins un bâtiment construit, une icône « + » apparaît au-dessus de la tuile : elle ouvre la liste des bâtiments disponibles (un seul type pour ce MVP) pour en déposer un.

### F5 — Placement de bâtiment

Conditions : joueur physiquement sur la tuile, tuile libre, bâtiment disponible dans l'inventaire. Effets : décrément du stock de bâtiments construits, création du document `buildings/{h3Index}` avec `lastCollectedAt = now`, apparition immédiate du modèle 3D sur la tuile. Cas particulier : le **tout premier placement** d'une partie est la base (offerte, voir game design) ; elle se comporte ensuite comme un extracteur normal.

### F6 — Rendu 3D des bâtiments

Chaque bâtiment placé est rendu par un modèle 3D (`model.glb`) posé sur le centre de la tuile via la **ModelLayer** de Mapbox, ancré au sol et teinté par une couleur d'identité par type (base ≠ extracteur). Le modèle est visible dès que la tuile entre dans le champ. Cette couche de rendu est isolée de la logique de position : passer à un autre modèle (art final) ne touche qu'elle. L'avatar, lui, reste un cube extrudé (FillExtrusionLayer), faute de modèle dédié.

### F7 — Inventaire et craft

Page accessible depuis la vue carte (bouton flottant). Deux onglets. **Onglet Ressources** : les 5 éléments avec leur quantité, mise à jour en temps réel (les extracteurs actifs font visiblement monter les compteurs). **Onglet Bâtiments** : le stock de bâtiments construits prêts à poser, et la zone de craft — la recette de l'extracteur, l'indication claire des ressources possédées vs requises, et un bouton « Construire » (instantané, pas de temps de fabrication pour ce MVP).

### F8 — Récolte et hors ligne

Aucun timer ne tourne en arrière-plan. La récolte est calculée **paresseusement** : à l'ouverture de l'app (et périodiquement pendant qu'elle est ouverte, ex. toutes les 30 s), pour chaque bâtiment, on calcule `gain = vitesse_extraction × (now − lastCollectedAt)`, on crédite l'inventaire et on avance `lastCollectedAt`. Comme les vitesses sont déterministes (recalculables depuis l'index H3), ce calcul ne dépend d'aucune donnée stockée par tuile. L'horloge utilisée est celle du client (acceptable pour ce prototype — l'anti-triche est hors périmètre ; le passage côté serveur est l'évolution naturelle post-MVP).

### F9 — Compte et persistance

Au premier lancement : création silencieuse d'un compte Firebase anonyme et du document joueur. Les écritures passent par le SDK Firestore (cache offline activé : l'app reste utilisable sans réseau, synchronisation au retour de connexion). Post-MVP, le compte anonyme sera lié à un SSO via l'account linking Firebase, sans perte de données.

## 5. Écrans

1. **Carte** (écran principal) : carte 3D, avatar, grille, bâtiments 3D, bouton inventaire, bouton recentrage.
2. **Panneau tuile** (bottom sheet sur la carte) : contenu de la tuile, action de placement le cas échéant.
3. **Inventaire** : onglets Ressources / Bâtiments (+ craft).
4. **Premier lancement** : écran minimal invitant à se rendre à l'endroit choisi et à poser sa base (un bouton « Poser ma base ici »).

## 6. Risques techniques identifiés (ce que le prototype doit révéler)

Le risque principal est la **précision GPS vs taille de tuile** : en zone urbaine dense, une précision de 10–20 m sur des tuiles de 50 m provoquera des changements de tuile erratiques ; le lissage (F2) et éventuellement une hystérésis (ne changer de tuile courante que si la nouvelle position y est franchement) sont les parades à tester. Viennent ensuite : la performance du rendu de la grille + modèles 3D sur des appareils Android moyens de gamme ; la consommation batterie (GPS continu + rendu carte) ; les quotas gratuits Mapbox/Firebase si le prototype circule au-delà de quelques testeurs ; et le caractère **expérimental de la ModelLayer** (désormais utilisée pour les bâtiments) — un `.glb` non rendable peut faire planter Mapbox nativement, d'où des modèles validés au préalable.

## 7. Décisions actées

- Android uniquement pour le prototype ; pas de SSO (auth anonyme) ; pas d'anti-triche ; gameplay réduit au strict minimum.
- H3 confirmé comme système de grille (résolution 11 provisoire).
- La base est un extracteur non déplaçable ; tout placement est permanent.
- Stockage et nombre d'extracteurs illimités ; récolte hors ligne créditée automatiquement.
- Pas de données de terrain réel dans la génération ; clusters naturels via bruit procédural.
- Le joueur démarre avec des ressources de base (quantités : voir document de game design).
- Bâtiments rendus par des modèles 3D (`.glb`) via la ModelLayer ; avatar rendu par un cube extrudé (FillExtrusionLayer) en attendant son propre modèle.
