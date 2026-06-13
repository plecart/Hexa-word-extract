# Hexa — Game design, algorithme de génération et équilibrage

**Version 0.1 — juin 2026 — document vivant**

⚠️ **Statut des chiffres** : l'équilibrage réel est explicitement reporté à l'après-MVP. Tous les nombres de ce document sont des **valeurs provisoires**, choisies pour que le prototype technique fonctionne et soit testable. Ils sont regroupés en constantes de configuration pour être modifiables en un seul endroit.

---

## 1. Boucle de jeu (MVP)

Le joueur se déplace dans le monde réel → il consulte les tuiles autour de lui pour repérer les plus riches → il crafte un extracteur depuis son inventaire → il se rend physiquement sur la tuile choisie et y pose l'extracteur → l'extracteur produit en continu, même application fermée → les ressources accumulées financent de nouveaux extracteurs → le joueur étend son réseau vers des tuiles plus riches et des éléments plus rares. L'unique objectif du MVP est l'accumulation.

L'amorçage (problème de la poule et de l'œuf) est résolu ainsi : le joueur reçoit un **kit de départ** de ressources, et son premier bâtiment — la **base** — est offert. La base est fonctionnellement un extracteur posé sur la tuile de départ.

## 2. Les cinq éléments

Par ordre de rareté croissante : **Cendrite, Givrelin, Lithosève, Échofer, Nyctite**. La rareté joue sur deux axes indépendants : la **probabilité de présence** sur une tuile, et la **vitesse d'extraction** quand l'élément est présent. Un élément rare est donc à la fois plus difficile à trouver et plus lent à récolter.

## 3. Algorithme de génération du monde

### 3.1 Principes

Trois exigences : **déterminisme** (la même tuile donne toujours le même contenu, sur tout appareil, sans rien stocker), **clusters naturels** (des gisements régionaux, pas un bruit blanc tuile par tuile), **simplicité** (pas de données de terrain réel).

La solution : l'index **H3** identifie la tuile ; le contenu est dérivé de la **position géographique du centre de la tuile** passée dans un **bruit simplex 3D** échantillonné sur la sphère terrestre. Travailler sur la sphère (et non en lat/lng plat) évite toute couture aux antiméridiens et toute distorsion aux pôles.

### 3.2 Pipeline (pour une tuile et un élément donnés)

```
1. (lat, lng) = h3.cellToLatLng(h3Index)            // centre de la tuile
2. (x, y, z)  = vecteur unitaire sur la sphère :
      x = cos(lat)·cos(lng) ; y = cos(lat)·sin(lng) ; z = sin(lat)
3. n = simplex3D(x·f, y·f, z·f  + offset_élément)    // n ∈ [−1, 1]
4. v = (n + 1) / 2                                   // v ∈ [0, 1]
5. présent      = v > seuil_élément
6. richesse     = (v − seuil) / (1 − seuil)          // ∈ ]0, 1] si présent
7. vitesse/h    = arrondi( taux_base_élément × (0,2 + 0,8 × richesse) )
```

Chaque élément possède son propre `offset` (champs de bruit indépendants : les gisements des cinq éléments ne se superposent pas systématiquement) et sa propre fréquence `f` (taille de cluster).

Le **seed global** du monde (qui décale tous les offsets) est une constante de l'application : tout le monde partage le même monde, condition indispensable au futur multijoueur.

### 3.3 Taille des clusters

La fréquence `f` se déduit de la longueur d'onde voulue : `f = circonférence_terrestre / longueur_d'onde`. Les éléments communs forment de vastes régions, les rares de petites poches — ce qui crée une raison de se déplacer loin.

| Élément | Longueur d'onde du gisement (provisoire) | Effet recherché |
|---|---|---|
| Cendrite | ~2 000 m | présent presque partout, en grandes nappes |
| Givrelin | ~1 500 m | régions larges, trous fréquents |
| Lithosève | ~1 000 m | gisements de quartier |
| Échofer | ~600 m | poches localisées |
| Nyctite | ~400 m | micro-gisements à dénicher |

Une seconde octave de bruit (amplitude 0,25, fréquence ×4) est ajoutée à chaque champ pour casser la régularité des contours sans changer la structure.

### 3.4 Seuils de présence et conséquences

Seuils **recalés empiriquement** (issue #16) sur un échantillon de ~24 000 cellules : la distribution du simplex n'étant pas uniforme, les seuils sont ajustés pour que la présence mesurée colle aux fréquences visées (mesure reproductible : `./gradlew :domain:worldDistributionReport`). Présence mesurée ci-dessous entre parenthèses ; l'équilibrage fin reste post-MVP.

| Élément | Seuil | Présence visée (mesurée) |
|---|---|---|
| Cendrite | 0,474 | ~55 % des tuiles (55,1 %) |
| Givrelin | 0,604 | ~30 % (29,7 %) |
| Lithosève | 0,698 | ~15 % (14,9 %) |
| Échofer | 0,769 | ~7 % (6,6 %) |
| Nyctite | 0,825 | ~3 % (2,7 %) |

Conséquences attendues : ~1,1 élément par tuile en moyenne ; environ **un quart des tuiles ne contiennent rien** (souhaité : le vide donne de la valeur au plein) ; une tuile à 4 ou 5 éléments est un événement rarissime (« jackpot » naturel).

## 4. Extraction

### 4.1 Vitesse

`vitesse (unités/heure) = taux_base × (0,2 + 0,8 × richesse)`. Le plancher de 20 % garantit qu'un gisement, même pauvre, produit toujours quelque chose ; la richesse fait varier le rendement d'un facteur 5 entre le pire et le meilleur gisement d'un même élément — de quoi motiver la prospection.

Taux de base provisoires (à richesse maximale) :

| Élément | Taux de base (u/h) | Fourchette réelle (u/h) |
|---|---|---|
| Cendrite | 60 | 12 – 60 |
| Givrelin | 30 | 6 – 30 |
| Lithosève | 14 | 2,8 – 14 |
| Échofer | 6 | 1,2 – 6 |
| Nyctite | 2 | 0,4 – 2 |

Un extracteur récolte **simultanément tous les éléments présents** sur sa tuile, chacun à sa vitesse propre. Production infinie : la tuile ne s'épuise jamais (décision actée), seule la vitesse compte.

### 4.2 Récolte hors ligne (calcul paresseux)

Aucun processus ne tourne en arrière-plan. Pour chaque bâtiment : `gain_élément = vitesse_élément × (now − lastCollectedAt)`, crédité à l'inventaire, puis `lastCollectedAt ← now`. Le déterminisme de la génération rend ce calcul exact quel que soit le temps écoulé, sans état intermédiaire. Pas de plafond hors ligne pour ce MVP (stockage illimité acté). Horloge client, triche tolérée (hors périmètre).

## 5. Économie du MVP

### 5.1 Recette de l'extracteur (provisoire)

Contrainte actée : bâtiment de base, exactement **2 minerais différents** — les deux plus communs.

> **Extracteur = 100 Cendrite + 40 Givrelin** (craft instantané)

### 5.2 Kit de départ (provisoire)

> **250 Cendrite + 100 Givrelin** — soit de quoi crafter **2 extracteurs immédiatement**, avec un reliquat.

Plus la **base offerte**, posée au premier lancement, qui agit comme extracteur sur sa tuile.

### 5.3 Rythme de progression qui en découle (à titre indicatif)

Avec la base + 2 extracteurs de départ sur des tuiles moyennes (richesse ~0,5 ; production Cendrite ~36 u/h et Givrelin ~18 u/h par tuile productrice), le 4ᵉ extracteur arrive en **2 à 4 heures** de production, le rythme s'accélérant ensuite avec chaque pose (croissance douce, typique d'un idle). Ces durées découlent mécaniquement des constantes ci-dessus ; ce sont elles qu'on ajustera lors du vrai chantier d'équilibrage post-MVP, une fois la boucle technique validée.

## 6. Constantes de configuration (récapitulatif)

```
H3_RESOLUTION            = 11
WORLD_SEED               = <constante globale>
WAVELENGTHS_M            = [2000, 1500, 1000, 600, 400]
PRESENCE_THRESHOLDS      = [0.474, 0.604, 0.698, 0.769, 0.825]   ; recalés empiriquement (#16)
BASE_RATES_PER_HOUR      = [60, 30, 14, 6, 2]
RATE_FLOOR               = 0.20
OCTAVE2_AMPLITUDE        = 0.25   ; OCTAVE2_FREQ_MULT = 4
RECIPE_EXTRACTEUR        = { cendrite: 100, givrelin: 40 }
STARTER_KIT              = { cendrite: 250, givrelin: 100 }
COLLECT_REFRESH_SECONDS  = 30
```

## 7. Chantier d'équilibrage post-MVP (mémo)

Questions volontairement laissées ouvertes, à reprendre une fois la technique validée : durée et fréquence de session cibles ; courbe de coût des extracteurs suivants (coût croissant ? plafond ?) ; épuisement ou capacité de stockage des tuiles ; retrait/déplacement des bâtiments ; rôle propre de la base ; arbres de craft utilisant Lithosève/Échofer/Nyctite (aujourd'hui accumulés sans usage — assumé pour le MVP) ; mesure empirique de la distribution réelle du bruit et recalage des seuils ; passage des calculs de récolte côté serveur (prérequis anti-triche et multijoueur).
