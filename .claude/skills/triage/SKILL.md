---
name: triage
description: Trie les issues à travers une machine à états pilotée par des rôles de triage. À utiliser quand l'utilisateur veut créer une issue, trier des issues, examiner les bugs ou demandes de fonctionnalités entrants, préparer des issues pour un agent AFK, ou gérer le workflow des issues.
---

# Triage

Faire progresser les issues sur GitHub (via `gh`) à travers une petite machine à états composée de rôles de triage.

Chaque commentaire ou issue publié sur GitHub (via `gh`) pendant le triage **doit** commencer par ce disclaimer :

```
> *Généré par IA pendant le triage.*
```

## Documents de référence

- [brief-agent.md](brief-agent.md) — comment rédiger des briefs d'agent durables
- [hors-scope.md](hors-scope.md) — comment fonctionne la base de connaissances `.out-of-scope/`
- [themes-milestones.md](themes-milestones.md) — comment regrouper les issues par thème via les milestones GitHub

## Rôles

Deux rôles de **catégorie** :

- `bug` — quelque chose est cassé
- `enhancement` — nouvelle fonctionnalité ou amélioration

Cinq rôles d'**état** :

- `needs-triage` — le mainteneur doit évaluer
- `needs-info` — en attente d'informations complémentaires de la part du rapporteur
- `ready-for-agent` — entièrement spécifié, prêt pour un agent AFK
- `ready-for-human` — nécessite une implémentation humaine
- `wontfix` — ne sera pas traité

Chaque issue triée doit porter exactement un rôle de catégorie et un rôle d'état. Si les rôles d'état entrent en conflit, signale-le et demande au mainteneur avant de faire quoi que ce soit d'autre.

Ce sont les noms de rôles canoniques ; le mapping vers les vrais labels GitHub est dans `.claude/pipeline.config.md` — sinon lancer `config-pipeline`.

## Thèmes (milestones)

En plus des rôles, chaque issue qui avance dans le triage est rattachée à **exactement un thème** —
un regroupement par sujet matérialisé par un **milestone GitHub** (purement thématique, sans date).
Cela permet de regrouper les issues d'un même thème. C'est **obligatoire pour toute issue triée,
sauf celles fermées en `wontfix`**. Voir [themes-milestones.md](themes-milestones.md) pour le choix
du thème, le nommage et les commandes `gh`.

Transitions d'état : une issue sans label passe normalement d'abord à `needs-triage` ; de là, elle évolue vers `needs-info`, `ready-for-agent`, `ready-for-human` ou `wontfix`. `needs-info` revient à `needs-triage` une fois que le rapporteur a répondu. Le mainteneur peut passer outre à tout moment — signale les transitions qui paraissent inhabituelles et demande avant de continuer.

## Invocation

Le mainteneur invoque `/triage` et décrit ce qu'il veut en langage naturel. Interprète la demande et agis. Exemples :

- « Montre-moi tout ce qui requiert mon attention »
- « Regardons l'issue #42 »
- « Passe l'issue #42 en ready-for-agent »
- « Qu'est-ce qui est prêt à être pris par les agents ? »

## Montrer ce qui requiert l'attention

Interroge GitHub (via `gh`) et présente trois catégories, des plus anciennes aux plus récentes :

1. **Sans label** — jamais triées.
2. **`needs-triage`** — évaluation en cours.
3. **`needs-info` avec activité du rapporteur depuis les dernières notes de triage** — nécessite une réévaluation.

Affiche les décomptes et un résumé d'une ligne par issue, en indiquant le thème (milestone) de chaque issue quand il y en a un. Si le mainteneur le demande, regroupe l'affichage par thème. Laisse le mainteneur choisir.

## Trier une issue spécifique

1. **Rassembler le contexte.** Lis l'issue complète (corps, commentaires, labels, milestone, rapporteur, dates). Analyse les éventuelles notes de triage antérieures pour ne pas reposer des questions déjà résolues. Explore le code en t'appuyant sur le glossaire métier du projet, en respectant les ADR du domaine concerné. Lis `.out-of-scope/*.md` et fais remonter tout rejet antérieur ressemblant à cette issue. Liste les **thèmes (milestones) existants** pour pouvoir réutiliser un thème en place plutôt que d'en créer un en double ([themes-milestones.md](themes-milestones.md)).

2. **Recommander.** Indique au mainteneur ta recommandation de catégorie, d'état et de **thème** (un milestone existant qui colle, ou un nouveau thème à créer) avec ton raisonnement, ainsi qu'un bref résumé du code pertinent pour l'issue. Attends ses instructions.

3. **Reproduire (bugs uniquement).** Avant tout interrogatoire, tente de reproduire : lis les étapes du rapporteur, trace le code concerné, lance des tests ou des commandes. Rapporte ce qui s'est passé — reproduction réussie avec le chemin de code, reproduction échouée, ou détails insuffisants (un fort signal de `needs-info`). Une reproduction confirmée permet un brief d'agent bien plus solide.

4. **Interroger (si nécessaire).** Si l'issue a besoin d'être étoffée, lance une session du skill `interroge-moi`.

5. **Appliquer le résultat :**

   Sauf pour `wontfix`, **rattache l'issue à son thème** avant tout autre changement : réutilise un
   milestone existant qui correspond, ou crée-en un automatiquement si aucun ne convient (annonce
   alors le thème créé au mainteneur). Voir [themes-milestones.md](themes-milestones.md).

   - `ready-for-agent` — publie un commentaire de brief d'agent ([brief-agent.md](brief-agent.md)).
   - `ready-for-human` — même structure qu'un brief d'agent, mais précise pourquoi cela ne peut pas être délégué (jugements à porter, accès externe, décisions de conception, tests manuels).
   - `needs-info` — publie des notes de triage (modèle ci-dessous).
   - `wontfix` (bug) — explication polie, puis ferme.
   - `wontfix` (enhancement) — écris dans `.out-of-scope/`, fais-y référence depuis un commentaire, puis ferme ([hors-scope.md](hors-scope.md)).
   - `needs-triage` — applique le rôle. Commentaire optionnel s'il y a un avancement partiel.

## Changement d'état rapide

Si le mainteneur dit « passe l'issue #42 en ready-for-agent », fais-lui confiance et applique le rôle directement. Confirme ce que tu t'apprêtes à faire (changements de rôle, commentaire, fermeture), puis agis. Saute l'interrogatoire. Si tu passes à `ready-for-agent` sans session d'interrogatoire, demande s'il souhaite que tu rédiges un brief d'agent.

Même en mode rapide, si l'état cible n'est pas `wontfix` et que l'issue n'a pas encore de thème, rattache-la à un thème (existant ou créé à la volée) dans la même opération ([themes-milestones.md](themes-milestones.md)).

## Modèle needs-info

```markdown
## Notes de triage

**Ce que nous avons établi jusqu'ici :**

- point 1
- point 2

**Ce dont nous avons encore besoin de ta part (@rapporteur) :**

- question 1
- question 2
```

Consigne tout ce qui a été résolu pendant l'interrogatoire sous « établi jusqu'ici » afin que le travail ne soit pas perdu. Les questions doivent être précises et actionnables, pas « merci de fournir plus d'informations ».

## Reprendre une session précédente

Si des notes de triage antérieures existent sur l'issue, lis-les, vérifie si le rapporteur a répondu à des questions en suspens, et présente un état des lieux actualisé avant de continuer. Ne repose pas les questions déjà résolues.
