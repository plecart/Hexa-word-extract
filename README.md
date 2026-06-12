# moon-pipeline-dev

Template et bibliothèque de skills pour une **pipeline de développement pilotée par les issues**,
100 % en français. Sert de **seed** : on l'amorce dans chaque nouveau projet via le skill
`init-projet`.

- **GitHub** = issues + PR (machine à états de triage).
- **Parallélisation** = git worktrees + sessions multiples.

## Le flux complet

```
[PRD]             [GitHub issues]                         [git worktrees]      [GitHub PR]
 vers-prd ──────▶ vers-issues ─▶ triage ─▶ ready-for-agent ─pr-paralleles─▶ cycle-pr ─▶ PR ─▶ QA
(conversation)   (tranches        (machine   (agent brief)   (1 worktree     (TDD +        (plan-qa →
                  verticales)      à états)                   par issue)      cleanup)      execution-qa →
                                                                                            bug-vers-issue)
                  ◀── repercussions : la clôture d'une issue réexamine & ajuste les autres ──┘
```

## Les skills

| Skill | Rôle |
|---|---|
| `init-projet` | Amorce un projet : git + GitHub, installe skills.sh, copie le template, déploie les skills maison, lance `config-pipeline`. |
| `config-pipeline` | Mapping rôle canonique → label GitHub, dépôt cible, liste de skills. Persisté dans `.claude/pipeline.config.md`. |
| `vers-prd` | Conversation → PRD. |
| `vers-issues` | PRD/plan → issues « tranches verticales » indépendantes. |
| `triage` | Machine à états des issues + regroupement par thème (milestones GitHub) + rédaction de l'« agent brief ». |
| `interroge-moi` | Interrogatoire pour lever toutes les ambiguïtés d'un plan. |
| `cycle-pr` | **Pièce centrale** : briefing → TDD red-green-refactor → cycle `modif→test→cleanup→test→commit` → auto-review → PR → review → merge → `repercussions`. Cleanup pass **verbatim**. |
| `repercussions` | À la clôture d'une issue, repasse sur les autres issues ouvertes : la résolution bouleverse-t-elle la suite (découpage, hypothèses, contrats, ordre, doublons) ? Propose les corrections, applique sur permission. |
| `pr-paralleles` | Plusieurs PR en parallèle via git worktrees. |
| `plan-qa` | Écrit un plan de QA (issue à cocher). |
| `execution-qa` | Déroule le plan de QA. |
| `bug-vers-issue` | Transforme un bug trouvé en issue GitHub. |

## Le template (`template/`)

Copié dans chaque projet par `init-projet` :

- `CLAUDE.md` — importe les règles permanentes via `@.claude/rules/...`
- `.claude/rules/` — `contraintes.md`, `cleanup-verbatim.md`, `taille-pr.md`
- `.claude/settings.json` — permissions de base
- `CONTRIBUTING.md` — pointeur humain vers `cycle-pr`
- `.github/workflows/ci.yml` — CI Python/`uv` (ruff + mypy + pytest, couverture ≥ 80 %)

## Conventions verrouillées

- **Langue** : skills, issues, PRD, descriptions → français. Commits/PR : préfixe conventional EN
  (`feat`/`fix`/`chore`/`docs`/`refactor`) + description FR. Ex : `feat(auth): ajoute la connexion`.
- **Relecture** : le prompt de cleanup s'exécute **mot pour mot** (`.claude/rules/cleanup-verbatim.md`).
- **Conception** : code **modulaire, scalable, fractionné, divisé** — modules profonds à
  responsabilité unique, extension plutôt que modification (`contraintes.md`).
- **Intégrité** : aucun commit cassé, jamais d'auto-merge, jamais de force-push sur `main`.
- **Doc à jour** : `README`, `docs/`, `CLAUDE.md`, glossaire et docstrings sont mis à jour **dans
  le même commit** que le changement de comportement/interface/architecture (`contraintes.md`).

## Démarrer un projet

Dans un nouveau projet, lancer le skill `init-projet` et se laisser guider (création du dépôt,
sélection des skills à cocher, mise en place de la config). Puis `vers-prd` ou `vers-issues` pour
commencer à planifier.
