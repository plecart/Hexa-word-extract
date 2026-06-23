# Pipeline de développement

Ce projet suit une pipeline de dev pilotée par les issues. Le cycle de réalisation d'une PR est
décrit dans [CONTRIBUTING.md](CONTRIBUTING.md) et exécutable via le skill `cycle-pr`.

## Règles permanentes (toujours actives)

@.claude/rules/contraintes.md
@.claude/rules/taille-pr.md
@.claude/rules/cleanup-verbatim.md
@.claude/rules/tests-ui-compose.md

## Vocabulaire

Utiliser le glossaire de domaine du projet (`UBIQUITOUS_LANGUAGE.md` s'il existe) dans les
noms de tests, les titres d'issues et de PR. Respecter les ADR de la zone touchée.

## Skills de la pipeline

`config-pipeline` (à lancer en premier), `vers-prd`, `vers-issues`, `triage`, `interroge-moi`,
`cycle-pr`, `repercussions`, `pr-paralleles`, `plan-qa`, `execution-qa`, `bug-vers-issue`,
`revue-codebase`.
