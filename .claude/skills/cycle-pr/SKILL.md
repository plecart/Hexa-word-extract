---
name: cycle-pr
description: Exécute le cycle complet de développement d'une PR — briefing pré-PR, implémentation en TDD red-green-refactor, cycle de commit modif→test→cleanup→test→commit, auto-review, ouverture de PR, boucle de review et merge. Utiliser pour implémenter une issue ready-for-agent, développer une fonctionnalité ou corriger un bug en suivant la pipeline.
---

# Cycle PR

Le cycle de réalisation d'une Pull Request, de bout en bout. Pièce centrale de la pipeline :
prend une issue `ready-for-agent` (ou une demande directe) et la mène jusqu'au merge — puis
répercute la clôture sur les autres issues — sans jamais laisser entrer un commit cassé dans
l'historique.

Les **règles permanentes** (`.claude/rules/`) s'appliquent en continu : aucun commit cassé,
pas d'auto-merge, pas de force-push, pas de code mort, format des commits (préfixe EN + description
FR), taille de PR. La **règle d'or** : au moindre doute, on s'arrête et on pose la question.

## Étape 1 — Briefing pré-PR (avant la première ligne de code)

Produire un briefing écrit AVANT de créer la branche :

- **Liste précise des fichiers** à créer/modifier — le chemin exact de chacun.
- **Décisions verrouillées** — choix d'architecture / scope / format actés.
- **Questions résiduelles** — découpage, format des données, transitions d'état, comportements,
  edge cases.

Poser **un maximum de questions pour lever toute incertitude maintenant**. Si l'issue manque de
contexte, lancer `interroge-moi`. Une question posée maintenant coûte 30 secondes ; la même
ambiguïté après 5 commits coûte une demi-journée.

### Auto-challenge du plan — le plan est-il encore exact ?

Avant de présenter le briefing comme acté, **se challenger explicitement** : le plan a pu être
écrit (issue, agent brief, conversation) *avant* que d'autres PR ne soient mergées. Le terrain a
peut-être bougé. Confronter le plan à la réalité **actuelle** du code :

- **Relire l'état réel** de la zone touchée (`git log` récent sur `main`, fichiers concernés) — pas
  l'état supposé au moment où l'issue a été écrite.
- **Pour chaque décision verrouillée et chaque fichier listé**, vérifier qu'elle tient toujours :
  l'interface existe-t-elle encore sous cette forme ? une PR récente a-t-elle déjà fait une partie
  du travail, déplacé un module, changé un contrat, rendu une hypothèse caduque ?
- **Vérifier que la suite du dev tient** : ce que cette PR prépare pour les issues suivantes est-il
  toujours cohérent, ou l'ordre / le découpage doit-il être revu ?
- **Énoncer à voix haute** sous un titre `## 🔍 Auto-challenge du plan` : ce qui reste valable, ce
  qui a changé, et les ajustements proposés (fichiers en plus/en moins, décision à rouvrir, scope à
  resserrer ou re-découper).

S'il y a un écart qui change le cours du dev, **s'arrêter et le remonter à l'utilisateur** avant le
« go » — ne pas dérouler un plan périmé en silence. Si tout tient, le dire explicitement (« plan
confirmé contre l'état actuel »).

Attendre un **« go » explicite**, puis créer la branche (trunk-based depuis `main`) :

```
git checkout -b <type>/<description>   # feat/  fix/  chore/  docs/  refactor/
```

## Étape 2 — Le cycle de commit (jamais sauté, jamais raccourci)

Chaque commit suit **exactement** :

```
1. modif (TDD)  →  2. test (vert)  →  3. cleanup pass  →  4. test (vert)  →  5. commit
```

### 2.1 Modification — en TDD (tracer bullet vertical)

Implémenter **un comportement à la fois** en red-green-refactor. **Ne jamais** écrire tous les
tests d'abord puis toute l'implémentation (slicing horizontal = mauvais tests) :

```
ROUGE : écrire UN test du prochain comportement → il échoue
VERT  : écrire le minimum de code pour le faire passer → il passe
```

Règles : un test à la fois ; juste assez de code pour passer le test courant ; ne pas anticiper
les tests futurs ; tester le comportement observable via l'interface publique.

- Bons vs mauvais tests : [references/tests.md](references/tests.md)
- Quand mocker (frontières du système uniquement) : [references/mocking.md](references/mocking.md)
- Concevoir des interfaces testables : [references/design-interface.md](references/design-interface.md)
- Viser des modules profonds : [references/modules-profonds.md](references/modules-profonds.md)

**Documentation maximale** : chaque fonction écrite est documentée à fond (intention, params,
valeur de retour, erreurs, effets de bord, pré/postconditions, exemples, edge cases) au format
idiomatique du langage. La doc explique le contrat, les tests prouvent le comportement.

**Doc projet en sync** : si le delta change un comportement, une commande, une interface ou
l'architecture, mettre à jour la doc concernée (`README`, `docs/`, `CLAUDE.md`, glossaire) **dans
le même commit** — voir `.claude/rules/contraintes.md`. La doc ne se met jamais à jour « plus tard ».

### 2.2 Premier passage de tests

Lancer la suite rapide. **Si rouge → pas de commit.** On répare.

### 2.3 Le « cleanup pass » — relecture distincte du delta

Une fois le delta fonctionnel et vert, faire une **relecture distincte** (pas du nettoyage au fil
de l'eau). Exécuter **verbatim** le prompt de relecture :

> Veuillez examiner l'ensemble du code ajouté et des modifications apportées aux fichiers
> existants. Réorganisez, structurez, optimisez et nettoyez ces modifications pour produire un
> code clair, cohérent et facilement maintenable. Éliminez tout code dupliqué, inutile ou
> obsolète. Décomposez les fonctions et séparez les responsabilités en suivant les principes
> KISS, DRY et YAGNI. L'objectif est d'obtenir un code propre, clair, concis et optimisé.

Règles de l'étape (voir aussi `.claude/rules/cleanup-verbatim.md`) :

- **Examen fichier par fichier.** Pour chaque fichier modifié, lister les findings concrets :
  magic numbers, duplication, naming douteux, fonction trop longue, responsabilité mal séparée,
  code mort. Pistes de refactor : [references/refactoring.md](references/refactoring.md).
- **Rapporter à voix haute** sous un titre `## 🧹 Cleanup pass`, pour vérification en un coup d'œil.
- **Même rigueur sur les commits triviaux.** Si « rien à corriger », l'expliquer ligne par ligne.
- **PR « foundation »** : YAGNI peut être suspendu ponctuellement (scaffold pour la suite), mais
  le **noter explicitement**. KISS, DRY et la structure restent actifs.

### 2.4 Second passage de tests

Le cleanup a pu casser quelque chose. Relancer les tests → **vert obligatoire**.

### 2.5 Commit

Uniquement si tout passe. Message = **titre conventional commit seul** (préfixe EN, description FR) :

```
git commit -m "type(scope): description"
```

Pas de corps, pas de footer, pas de `Co-Authored-By`, pas de numéro de PR. Breaking change : `!`
dans le titre. Puis enchaîner sur le commit suivant (retour 2.1).

### Point d'arrêt humain

Si le commit touche l'**UI à valider visuellement** ou les **models / migrations**, rendre la
main pour validation manuelle **avant** de committer.

## Étape 3 — Auto-review de fin de PR (avant `git push`)

Quand tous les commits sont faits, revue critique du **diff complet de la branche** (`main..HEAD`),
en vision d'ensemble cette fois. Chercher : problèmes d'architecture (couplage, frontières),
naming incohérent entre commits, edge cases / erreurs / race conditions, KISS/DRY/YAGNI à
l'échelle de la PR — notamment la **duplication inter-commits**.

Chaque problème trouvé → **un commit de fix dédié** (qui suit lui aussi le cycle complet 2.1→2.5).
Une fois l'auto-review propre → `git push`.

## Étape 4 — Ouverture de la PR

Dès la création, la PR a :

- **Titre** conventional commit (préfixe EN + description FR).
- **Lien d'issue** : le body **doit** contenir une ligne de clôture GitHub `Closes #N` — mot-clé
  **anglais** (`Closes` / `Fixes` / `Resolves`), **hors backticks**, **jamais en français**
  (`Ferme` / `Clôt` ne déclenchent pas l'auto-link et laissent la PR orpheline). PR en deux
  tranches : seule la **dernière** clôt (`Closes #N`) ; la première **référence sans fermer**
  (`Tranche 1/2 de #N`, sans mot-clé de clôture).
  **Exception tooling/meta** : une PR purement outillage/pipeline (skills, `.claude/rules`,
  `.gitignore`, config CI…) qui ne correspond à aucune issue n'a **pas** de `Closes #N` — c'est
  un cas légitime, pas une erreur à corriger.
- **Assignee** : le mainteneur responsable (par défaut, le propriétaire du repo).
- **Labels** : **uniquement un label de type** (`enhancement` / `bug` / `documentation`).
  **Aucun label de workflow de triage** sur une PR (`ready-for-agent`, `ready-for-human`,
  `needs-triage`, `needs-info`, `qa-plan`, `qa-finding`) : ces labels décrivent l'état d'une
  **issue** dans la machine de triage et n'ont aucun sens sur une PR. Cf. `.claude/pipeline.config.md`.
- **Pas de milestone sur une PR.** Le jalon vit sur l'**issue** ; le lien `Closes #N` rattache
  déjà la PR au jalon. Ne jamais recopier le milestone de l'issue sur la PR.
- Un **body structuré** :

```markdown
## Summary
<1-3 phrases : l'intention de la PR>

Closes #N   <!-- « Tranche 1/2 de #N » si PR non-finale ; ligne omise si PR tooling/meta sans issue -->

## Changes
<commits regroupés par thème : infra / feature / tests / docs>

## Test plan
- [ ] CI verte
- [ ] <vérifs manuelles / validation device si pertinent>

## Notes
<décisions, follow-ups différés, hors-scope>
```

Les cases du Test plan sont **non cochées à l'ouverture** (le travail de vérif n'est pas encore
fait) et se cochent **au fil de l'eau** ; l'étape 6 vérifie qu'aucune ne reste vide avant le merge.

Au-delà de **~10 fichiers** ou **~500 lignes** de diff, découper la PR.

## Étape 5 — Review + CI

- **CI verte** : lint + format + typecheck + tests, couverture ≥ 80 %.
- **Revue** : revue automatisée (bot) ou self-audit ligne par ligne (sécurité, correctness,
  lisibilité, KISS/DRY/YAGNI, cohérence avec la stack).
- Chaque commentaire **pertinent** → cycle complet (2.1→2.5) + push + re-trigger de la review.
  Chaque commentaire **non pertinent** → noter brièvement pourquoi, ne pas corriger.
- **Boucler jusqu'à review vide.**

## Étape 6 — Avant le merge

Après le « go merge » humain, **avant de merger** : un **dernier cleanup pass holistique** sur le
diff complet de la branche (même prompt verbatim qu'en 2.3, mais à l'échelle de la PR). C'est le
filet final pour les findings inter-commits.

- Findings → présenter, corriger (cycle complet), re-tester, push, *puis* merger.
- Sinon → verdict explicite « code propre, prêt à merger ».

**Hygiène finale de la PR avant merge** (relire la PR, pas seulement le code) :

- **Test plan** : cocher chaque case au fil de l'eau ; **avant de merger, aucune case ne reste
  `- [ ]`**. Une case non cochée = un travail non fait → soit on le fait, soit on le sort
  explicitement du périmètre (et on retire la case). Une PR ne se merge pas avec des cases vides.
- **Métadonnées** : assignee posé, **label de type seul** (aucun label de triage), **aucun
  milestone**, lien `Closes #N` présent (sauf PR tooling/meta). Corriger avant merge.
- **Retours de validation au bon endroit** : les retours de **validation device / QA / recette**
  (résultats du point d'arrêt humain) se consignent sur l'**issue** liée, **pas** en commentaire
  de la PR — l'issue est la mémoire durable du comportement ; la PR sort du flux après merge.

**Jamais d'auto-merge.** Même CI verte + audit propre, toujours attendre un « go » / « merge »
humain explicite.

## Étape 7 — Répercussions sur les autres issues (après le merge)

Le merge **clôt l'issue liée** : la PR et la conversation qui l'a résolue ont pu acter des
décisions, déplacer un contrat d'interface, déjà faire une partie d'un autre lot, ou rendre une
hypothèse caduque ailleurs. **Immédiatement après le merge**, lancer le skill `repercussions` — la
conversation qui a résolu l'issue est **encore en contexte**, c'est le moment où l'analyse est la
plus riche. Même pour une **demande directe** sans issue liée, faire la passe : le delta mergé
peut impacter des issues ouvertes.

Il confronte le **delta réel** de la PR à **toutes les autres issues ouvertes** (d'abord le même
thème, puis un balayage large), présente les issues impactées, et **n'édite/ferme/re-trie qu'après
un « go » explicite**. C'est le pendant *sortant* de l'auto-challenge *entrant* de l'Étape 1.

Ne pas considérer la PR « terminée » tant que cette passe n'a pas été faite (même si la conclusion
est « aucune répercussion »).

## Les 4 idées à retenir

1. Le cycle `modif → test → cleanup → test → commit` est **sacré et jamais raccourci** — y compris
   le cleanup sur les commits triviaux.
2. Tout l'effort de questions se fait **avant** de coder (briefing pré-PR).
3. **Deux filets de cleanup** : un par commit (regard local), un sur la PR entière avant merge
   (regard global, inter-commits).
4. La PR n'est **terminée qu'après la passe `repercussions`** : une clôture peut bouleverser les
   autres issues — on le vérifie pendant que la conversation est encore en contexte.
