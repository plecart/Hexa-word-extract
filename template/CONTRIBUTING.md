# Contribuer

Le développement suit une pipeline pilotée par les issues. **La source de vérité du process n'est
pas ce fichier** : c'est le skill `cycle-pr` (déroulé pas à pas) et les règles permanentes dans
[.claude/rules/](.claude/rules/). Ce document n'en donne que la carte.

## Le flux en une page

1. **Planifier** — `vers-prd` (conversation → PRD) → `vers-issues` (PRD → petites issues) →
   `triage` (machine à états, produit un « agent brief »). `interroge-moi` pour lever les doutes.
2. **Implémenter** — `cycle-pr` sur une issue `ready-for-agent` : briefing pré-PR → TDD
   red-green-refactor → cycle `modif → test → cleanup → test → commit` → auto-review → PR → review
   → merge. Plusieurs PR en parallèle : `pr-paralleles` (worktrees).
3. **Valider** — `plan-qa` → `execution-qa` → `bug-vers-issue`.

## Les règles qui ne se négocient pas

Voir [.claude/rules/](.claude/rules/) (chargées automatiquement via `CLAUDE.md`) :

- **Aucun commit cassé**, jamais d'auto-merge, jamais de force-push — `contraintes.md`
- **Cleanup pass verbatim** à chaque relecture (DRY / KISS / YAGNI) — `cleanup-verbatim.md`
- **Format des commits** : `type(scope): description`, préfixe EN + description FR ; PR ≤ ~10
  fichiers / ~500 lignes — `taille-pr.md`
