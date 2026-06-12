---
name: init-projet
description: Amorce un nouveau projet avec la pipeline de dev complète — git init + dépôt GitHub, installe la CLI skills.sh et télécharge les skills utiles (sélection à cocher), copie le template (.claude/rules, CLAUDE.md, CONTRIBUTING, CI), déploie les skills maison, et lance config-pipeline. Utiliser au tout début d'un projet, pour mettre en place la pipeline, ou « initialise le projet ».
---

# Init projet

Met en place, en une passe, toute la pipeline de dev dans un nouveau projet (ou un projet
existant qui n'a pas encore la pipeline). Source du template : ce dépôt `moon-pipeline-dev`
(dossier `template/` + dossier `skills/`).

## Process

### 1. Localiser le template

Déterminer le chemin du dépôt `moon-pipeline-dev` (le demander si nécessaire). On y lira
`template/` et `skills/`.

### 2. Initialiser git + le dépôt GitHub

- Si le projet n'est pas un dépôt git : `git init` + premier commit.
- Proposer de créer le dépôt GitHub : `gh repo create <owner>/<repo> --private --source=. --remote=origin`
  (demander public/privé). Si un remote existe déjà, le réutiliser.
- Brancher trunk-based sur `main`.

### 3. Copier le template dans le projet

Copier depuis `moon-pipeline-dev/template/` vers la racine du projet :

- `CLAUDE.md` (importe les règles via `@.claude/rules/...`)
- `.claude/rules/` (contraintes, cleanup-verbatim, taille-pr)
- `.claude/settings.json` (permissions de base ; fusionner s'il en existe déjà un)
- `CONTRIBUTING.md` (pointeur vers `cycle-pr`)
- `.github/workflows/ci.yml` (adapter à la stack détectée — voir étape 6)

### 4. Déployer les skills MAISON dans le projet

Copier les skills de la pipeline depuis `moon-pipeline-dev/skills/` vers **`.claude/skills/`** et
**`.agents/skills/`** du projet (mêmes emplacements que les projets existants) :

`config-pipeline`, `vers-prd`, `vers-issues`, `triage`, `interroge-moi`, `cycle-pr`,
`repercussions`, `pr-paralleles`, `plan-qa`, `execution-qa`, `bug-vers-issue`.

### 5. Installer la CLI skills.sh + télécharger les skills publics (sélection à cocher)

`skills.sh` est le gestionnaire de paquets de skills (`npx skills`).

1. Vérifier la CLI : `npx skills --version` (l'installer/initialiser au besoin).
2. Présenter à l'utilisateur la **liste proposée** (issue de `.claude/pipeline.config.md` si
   présent, sinon le set par défaut selon la stack) **sous forme de cases à cocher** — il
   active/désactive chaque skill. Set par défaut Python/Django :
   `django-tdd`, `python-testing-patterns`, `webapp-testing`, `find-skills`, `docker-patterns`,
   `accessibility`, `frontend-design`.
3. Proposer aussi une recherche : `npx skills find <terme>` pour en ajouter d'autres depuis le
   catalogue https://skills.sh/.
4. Installer la sélection : `npx skills add <pkg>` pour chaque skill coché.

Ne **jamais** tout installer sans confirmation : l'utilisateur coche d'abord.

### 6. Adapter le CI à la stack

- Détecter la stack (présence de `pyproject.toml`, `package.json`, etc.).
- Stack par défaut visée : **Python/Django avec `uv`** (le `ci.yml` du template est déjà calibré :
  ruff + mypy + pytest, couverture ≥ 80 %). Pour une autre stack, ajuster les étapes (les services
  Postgres/Redis sont en commentaire dans le template, à décommenter si besoin).

### 7. Lancer `config-pipeline`

Enchaîner sur le skill `config-pipeline` pour : fixer le mapping rôle→label, créer les labels
GitHub manquants, et enregistrer le dépôt.

### 8. Commit d'amorçage

Committer la mise en place :

```
git add -A && git commit -m "chore: amorçage pipeline de dev"
```

### 9. Récapituler

Afficher à l'utilisateur : dépôt GitHub créé, skills maison déployés, skills publics installés
(liste cochée), labels créés, CI en place, et les prochaines étapes (`vers-prd` ou `vers-issues`
pour démarrer la planification).

## Checklist finale

- [ ] git + remote GitHub en place, branche `main`
- [ ] `CLAUDE.md` + `.claude/rules/` + `settings.json` copiés
- [ ] `CONTRIBUTING.md` + `.github/workflows/ci.yml` copiés et CI adaptée à la stack
- [ ] skills maison déployés dans `.claude/skills/` et `.agents/skills/`
- [ ] CLI skills.sh OK + skills publics cochés installés
- [ ] `config-pipeline` exécuté (labels créés, config écrite)
- [ ] commit d'amorçage fait
