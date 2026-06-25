# Configuration pipeline

## Dépôt GitHub
- repo : `plecart/Hexa-word-extract`
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

## Portée des labels et milestones (issue vs PR)
- Labels de **workflow de triage** (`needs-triage`, `needs-info`, `ready-for-agent`,
  `ready-for-human`, `qa-plan`, `qa-finding`) : **issues uniquement**. Jamais sur une PR — ils
  décrivent l'état d'une issue dans la machine de triage.
- Labels de **type** (`bug`, `enhancement`, `documentation`) : issues **et** PR.
- **Milestone** : **issues uniquement**. Jamais sur une PR — le lien `Closes #N` rattache déjà la
  PR au jalon de son issue.
- **Lien PR → issue** : mot-clé de clôture **anglais** `Closes #N` (pas `Ferme`/`Clôt`, pas de
  backticks). Une PR purement outillage/pipeline sans issue est **exemptée** de ce lien.

## Langue
- skills / issues / PRD / descriptions : français
- préfixe conventional commit : anglais (feat/fix/chore/docs/refactor)

## Skills du projet
- Maison : config-pipeline, vers-prd, vers-issues, triage, interroge-moi, cycle-pr, repercussions, pr-paralleles, plan-qa, execution-qa, bug-vers-issue
- Publics (skills.sh) : affaan-m/everything-claude-code@android-clean-architecture, affaan-m/everything-claude-code@kotlin-testing, thebushidocollective/han@android-jetpack-compose
