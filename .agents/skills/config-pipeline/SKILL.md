---
name: config-pipeline
description: Configure la pipeline de dev d'un projet — établit le mapping entre les rôles canoniques et les labels GitHub réels, le dépôt cible, et la liste de skills à installer. À lancer en premier dans un projet, avant triage / vers-prd / vers-issues. Les autres skills réclament cette config.
---

# Config Pipeline

Établit et maintient la configuration de la pipeline pour le projet courant. Tous les skills de
planification (`triage`, `vers-prd`, `vers-issues`, `plan-qa`…) lisent cette config pour savoir
quels labels GitHub utiliser et sur quel dépôt agir.

La config est persistée dans **`.claude/pipeline.config.md`** à la racine du projet (versionnée).

## Quand l'utiliser

- Au démarrage d'un projet (souvent appelé par `init-projet`).
- Quand un autre skill signale « la config de pipeline n'a pas été fournie ».
- Pour ajuster le mapping de labels, le dépôt ou la liste de skills.

## Process

### 1. Lire la config existante

Si `.claude/pipeline.config.md` existe, le lire et présenter l'état actuel. Sinon, créer la config.

### 2. Déterminer le dépôt GitHub

- Lire `git remote -v`. Si un remote GitHub existe, le proposer comme dépôt cible (`owner/repo`).
- Sinon, demander à l'utilisateur le `owner/repo` (ou proposer d'en créer un — c'est le rôle de
  `init-projet`).

### 3. Établir le mapping de labels

Les skills raisonnent en **rôles canoniques**. Le dépôt utilise des **chaînes de labels** réelles.
Établir la correspondance (proposer ces valeurs par défaut, les créer dans GitHub si absentes via
`gh label create`) :

| Rôle canonique | Type | Label par défaut | Couleur |
|---|---|---|---|
| bug | catégorie | `bug` | `d73a4a` |
| enhancement | catégorie | `enhancement` | `a2eeef` |
| needs-triage | état | `needs-triage` | `fbca04` |
| needs-info | état | `needs-info` | `d4c5f9` |
| ready-for-agent | état | `ready-for-agent` | `0e8a16` |
| ready-for-human | état | `ready-for-human` | `1d76db` |
| wontfix | état | `wontfix` | `ffffff` |
| qa-plan | suivi | `qa-plan` | `0e8a16` |
| qa-finding | suivi | `qa-finding` | `b60205` |

Demander à l'utilisateur s'il veut des chaînes différentes (ex. labels existants du projet).

### 4. Liste de skills du projet

Enregistrer la liste de skills (publics skills.sh + maison) retenue pour ce projet. C'est cette
liste que `init-projet` propose à cocher. Set par défaut Python/Django : `django-tdd`,
`python-testing-patterns`, `webapp-testing`, `find-skills`, `docker-patterns`, `accessibility`,
`frontend-design`.

### 5. Écrire `.claude/pipeline.config.md`

Utiliser ce gabarit, puis confirmer à l'utilisateur :

<gabarit-config>
# Configuration pipeline

## Dépôt GitHub
- repo : `owner/repo`
- branche trunk : `main`

## Mapping labels (rôle canonique → label GitHub)
- bug → `bug`
- enhancement → `enhancement`
- needs-triage → `needs-triage`
- needs-info → `needs-info`
- ready-for-agent → `ready-for-agent`
- ready-for-human → `ready-for-human`
- wontfix → `wontfix`
- qa-plan → `qa-plan`
- qa-finding → `qa-finding`

## Langue
- skills / issues / PRD / descriptions : français
- préfixe conventional commit : anglais (feat/fix/chore/docs/refactor)

## Skills du projet
- <liste retenue>
</gabarit-config>

### 6. Créer les labels manquants dans GitHub

Pour chaque label du mapping absent du dépôt : `gh label create <nom> --description "<rôle>" --color <couleur>`.
Confirmer ce qui a été créé.

## Note pour les autres skills

Quand un skill a besoin du tracker ou du vocabulaire de labels, il lit
`.claude/pipeline.config.md`. S'il est absent, lancer `config-pipeline` d'abord.
