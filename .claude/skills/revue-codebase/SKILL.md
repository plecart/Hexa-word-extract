---
name: revue-codebase
description: Audit holistique en lecture seule de toute la codebase — produit un rapport de revue (architecture, KISS/DRY/YAGNI, code mort, doc périmée, scalabilité) sans jamais modifier le code, puis propose de consigner les findings en issues (routage vers-issues / bug-vers-issue). À utiliser pour une revue complète du code, un état des lieux qualité, « relis toute la codebase », ou « fais un audit du code ».
---

# Revue codebase

Audit **holistique** et **en lecture seule** de toute la base de code, qui produit un **rapport** —
jamais une modification. C'est le pendant *global* des deux filets de cleanup de `cycle-pr` (relecture
par commit, auto-review par PR) : là où `cycle-pr` examine un *delta*, ce skill relit **l'ensemble du
repo** d'un seul tenant, pour repérer ce qu'une revue de diff ne voit pas — dérive d'architecture,
duplication inter-modules, code rendu mort par une évolution, doc désynchronisée.

Les **règles permanentes** (`.claude/rules/`) servent de grille de lecture : modulaire / scalable /
fractionné, aucun code mort, doc à jour, KISS / DRY / YAGNI. Le **vocabulaire** suit le glossaire
métier (`UBIQUITOUS_LANGUAGE.md` s'il existe) et les ADR de la zone.

## Garde-fou central — lecture seule

- **Aucune modification du code applicatif.** Ce skill lit, analyse, rapporte. Il ne corrige rien :
  la correction passe par `cycle-pr` sur les issues qu'il aura aidé à créer.
- **Rien écrit sur disque.** Le rapport est affiché dans la conversation, pas sauvegardé en fichier.
- **Exécution autorisée en lecture seule.** Il **peut** lancer la suite de tests, le lint et le build
  (`./gradlew test`, ktlint, …) pour *étayer* un finding (code mort réel, compilation, couverture),
  mais jamais pour éditer quoi que ce soit.

## Périmètre

- **Par défaut : tout le repo** — tous les modules, code de production *et* tests.
- **Argument optionnel** : un chemin ou un module (`:app`, `app/src/main/.../map`) pour cibler la
  revue sur une zone. Sans argument → revue complète.

## Processus

### 1. Cartographier

Avant de lire le détail, comprendre la forme :

- Structure des modules et **graphe de dépendances** (fichiers de build, `settings.gradle.kts`).
- Conventions du projet : `CLAUDE.md`, `.claude/rules/`, glossaire métier, ADR.
- Historique récent (`git log --oneline -20`) pour repérer les évolutions qui ont pu laisser des
  résidus (code rendu mort, doc périmée).

### 2. Lire le code, fichier par fichier

**Examen fichier par fichier, pas un survol vague** (cf. `.claude/rules/cleanup-verbatim.md`). Pour
chaque fichier, dérouler la grille des 10 axes : [references/grille-revue.md](references/grille-revue.md).

Étayer si besoin en lançant tests / lint / build **en lecture seule** : confirmer qu'un symbole est
vraiment mort, que ça compile, où la couverture est faible. Ne jamais modifier.

### 3. Vérifier chaque finding avant de le reporter

**Ne rien affirmer sans preuve.** Avant d'écrire « code mort », « dupliqué » ou « périmé » :

- chercher les usages réels (recherche de symboles, références croisées) ;
- pour une référence d'issue périmée, vérifier que la tranche est bien livrée ;
- pour une duplication, montrer les deux emplacements.

Un finding non vérifié est un bruit qui décrédibilise le rapport.

### 4. Produire le rapport (affiché)

Structure :

- **Verdict global** — santé de l'architecture en 2-3 phrases.
- **Findings par sévérité**, chacun avec **référence `fichier:ligne` cliquable**, le problème, et la
  **recommandation** (et, si pertinent, le regroupement en PR potentielle) :
  - 🔴 **À traiter** — code mort / obsolète / cassant, violation nette d'une règle permanente.
  - 🟡 **À corriger** — dette, incohérence, duplication, doc périmée.
  - 🟢 **Polish** — placement, nommage, amélioration optionnelle.
- **Ce qui est bien** — les forces à préserver (pas seulement du négatif).
- **Rappel explicite** : aucune modification n'a été faite.

### 5. Proposer de consigner les findings en issues (optionnel)

Une fois le rapport affiché, **proposer** : « Veux-tu consigner des issues pour ces findings ? »
N'enchaîne jamais en silence.

**Regroupement en lots.** Regrouper les findings liés en **lots de travail cohérents** — un lot = une
issue (ex. « nettoyage post-GPS » = source simulée morte + références d'issues périmées). Préférer
peu d'issues actionnables à une nuée de micro-tickets.

**Sélection par défaut.** Proposer **🔴 + 🟡 cochés**, **🟢 décochés** (disponibles si l'utilisateur
les veut). L'utilisateur ajuste avant toute création.

**Anti-doublon (toujours).** Avant de rédiger quoi que ce soit, chercher les issues déjà ouvertes :
`gh issue list --search "<mots-clés du lot>" --state all --limit 10`. Montrer les correspondances
proches ; ne pas re-loguer un problème déjà ticketé. *(C'est ce qui rend ce skill ré-exécutable sans
polluer le backlog.)*

**Routage selon la nature du finding** :

- **Comportement / correctness** (défaut observable : cast dur qui peut planter, edge case, race
  condition) → déléguer à **`bug-vers-issue`** : gabarit défaut, sévérité, repro, traçabilité.
- **Structurel / refactor / dette** (code mort, duplication, doc périmée, placement, KISS/DRY/YAGNI)
  → déléguer à **`vers-issues`** : le lot sert de « plan » (déjà en contexte), découpé en issues avec
  critères d'acceptation.

**Le `fichier:ligne` va en piste, pas en contrat.** Dans le corps d'issue, mettre les références
`fichier:ligne` en *Notes / pistes de départ* — elles aident à démarrer mais deviennent obsolètes ;
le titre et le corps restent en langage métier (règle commune aux deux skills).

**Jamais de publication silencieuse.** Brouillon montré, « go » explicite requis, label `needs-triage`
(plus `qa-finding` pour la voie bug). Les issues entrent ensuite dans le flux normal :
`needs-triage → triage → ready-for-agent → cycle-pr`.

## Les idées à retenir

1. **Lecture seule, toujours** : ce skill rapporte, il ne corrige pas. La correction, c'est `cycle-pr`.
2. **Fichier par fichier**, findings vérifiés, références `fichier:ligne` — pas de survol vague.
3. **Pendant holistique** de `cycle-pr` : il voit la dette inter-modules qu'une revue de diff manque.
4. **Issues routées** : comportement → `bug-vers-issue`, structurel → `vers-issues`, anti-doublon
   dans les deux cas pour rester ré-exécutable.
