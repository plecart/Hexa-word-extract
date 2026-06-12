# Thèmes (milestones GitHub)

Pendant le triage, chaque issue est rattachée à un **thème** afin de regrouper les issues par
sujet. Le thème est matérialisé par un **milestone GitHub** : c'est l'unique source de vérité, il
n'y a aucune config ni fichier à maintenir en plus.

Les milestones sont utilisés ici de façon **purement thématique** : un nom et une description
courte, **aucune date d'échéance ni sémantique de release**. C'est un détournement assumé du
milestone GitHub comme étiquette de regroupement.

## Règle

- Toute issue qui **avance dans le triage** (`needs-triage`, `needs-info`, `ready-for-agent`,
  `ready-for-human`) est rattachée à **exactement un thème**.
- Une issue fermée en **`wontfix` est exemptée** — on ne lui assigne pas de thème.
- Comme un milestone GitHub est unique par issue, « exactement un thème » est garanti par
  construction.

## Choisir un thème

L'objectif est d'avoir **peu de thèmes larges et stables**, pas un thème par issue. Avant
d'assigner :

1. **Lister les milestones existants** et tenter une correspondance **par concept, pas par
   mot-clé** — « écran de connexion » correspond au thème `Authentification`, « lenteur au
   chargement » correspond à `Performance ». Réutilise un thème existant dès qu'il colle.
2. **Créer un thème seulement si aucun existant ne convient vraiment.** La création est
   automatique (pas de demande de permission), mais reste disciplinée pour éviter les doublons et
   les thèmes mal nommés. **Annonce systématiquement au mainteneur le thème créé** (transparence).

### Nommer un thème

- Nom court, lisible, reconnaissable, en **français** : `Authentification`, `Facturation`,
  `Performance`, `Imports CSV`. Quelqu'un qui parcourt la liste des milestones doit comprendre le
  périmètre sans ouvrir le thème.
- Description d'une ligne précisant ce que le thème regroupe (« Connexion, sessions, gestion des
  comptes et permissions »).

## Commandes (`gh`)

Le mapping rôle → label de `.claude/pipeline.config.md` ne concerne pas les thèmes ; les milestones
sont des objets GitHub natifs. Le dépôt cible (`owner/repo`) vient de cette même config.

```bash
# Lister les thèmes existants (titres + descriptions)
gh api "repos/{owner}/{repo}/milestones" --jq '.[] | "\(.title) — \(.description)"'

# Créer un thème (purement thématique, sans date)
gh api "repos/{owner}/{repo}/milestones" -f title="Authentification" \
  -f description="Connexion, sessions, gestion des comptes et permissions" -f state=open

# Rattacher une issue à un thème
gh issue edit <numéro> --milestone "Authentification"
```

`gh issue edit --milestone` exige que le milestone existe déjà : crée-le d'abord si nécessaire.

## Renommer ou fusionner des thèmes

Les thèmes dérivent avec le temps. Quand deux thèmes se recouvrent ou qu'un nom devient flou :

- Le mainteneur peut demander une fusion. Réassigne les issues du thème absorbé vers le thème
  cible (`gh issue edit --milestone`), puis ferme le milestone vidé.
- Ne supprime pas un milestone qui porte encore de l'historique utile ; ferme-le plutôt.

Ces opérations de réorganisation ne se font que **sur demande explicite** du mainteneur — l'auto
-création ne concerne que l'assignation d'une issue en cours de triage.
