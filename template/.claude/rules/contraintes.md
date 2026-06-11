# Règle — Contraintes permanentes (à chaque commit)

Ces contraintes sont actives en continu, sur **chaque** commit, sans qu'on ait à les rappeler.

## Intégrité de l'historique
- **Aucun commit cassé n'entre dans l'historique.** La qualité est garantie *à chaque commit*,
  pas seulement à la fin. Si les tests sont rouges → pas de commit.
- **Jamais de force-push** sur une PR ouverte (sauf rebase/squash explicitement demandé), et
  **jamais** sur `main`.
- **Jamais d'auto-merge.** Même avec CI verte et audit propre, toujours attendre un « go » /
  « merge » humain explicite.

## Propreté du code
- **Aucun code mort, aucun scaffold inutilisé.** Supprimer les imports / classes /
  commentaires-tutoriels générés par les outils ; ne garder que ce qui sert *maintenant*.
- **Pas de fichiers placeholder « au cas où ».**
- **Pas d'imports wildcard** (`from x import *`).
- **Pas de `print()` de debug** ni de `TODO`/`FIXME` sans propriétaire et explication.

## Qualité de conception (modulaire & scalable)
Le code visé est **modulaire, scalable, fractionné et divisé** — c'est un objectif permanent, pas
une option :

- **Modulaire** : découpler en modules à **responsabilité unique**, derrière des interfaces
  petites et stables. Viser des **modules profonds** (petite interface, implémentation riche, cf.
  `cycle-pr/references/modules-profonds.md`), pas des modules superficiels qui passent-plat.
- **Fractionné / divisé** : décomposer les fonctions et séparer les responsabilités (KISS/DRY/YAGNI).
  Pas de fonction fourre-tout, pas de fichier monolithique : préférer plusieurs petites unités
  ciblées, testables isolément, à une grosse unité qui fait tout.
- **Scalable** : concevoir pour que l'ajout d'un cas / d'un volume / d'une fonctionnalité se fasse
  par **extension** (nouveau module, nouvelle implémentation derrière l'interface) plutôt que par
  modification invasive du code existant. Éviter le couplage fort et les hypothèses qui ne tiennent
  qu'à petite échelle (boucles N+1, état global, limites en dur non justifiées).
- **À chaque cleanup pass**, vérifier explicitement ces 3 axes en plus de KISS/DRY/YAGNI.

## Règle d'or — toujours poser un maximum de questions
- **Poser systématiquement un maximum de questions pour lever toute incertitude**, le plus tôt
  possible. Une question posée maintenant coûte 30 secondes ; la même ambiguïté découverte plus
  tard coûte une demi-journée.
- **Au moindre doute, on s'arrête et on demande.** Jamais de supposition silencieuse sur le scope,
  le format des données, les transitions d'état, les comportements attendus.
- **Toute prise de décision non triviale dans le doute → demander avant de trancher.** Choix
  d'architecture, de découpage, de format, de dépendance, de comportement : on ne décide pas en
  silence à la place de l'utilisateur. Présenter les options + une reco, puis demander.
- Exception : si la réponse se trouve dans le code, **explorer le code plutôt que demander**. Pour
  un grilling approfondi, utiliser `interroge-moi`.

## Maintenance continue
- Tenir à jour, sans qu'on le redemande, les supports de suivi : Makefile, `.env.example`,
  todo-list, tableau de suivi projet.
- **Tenir la documentation à jour dans le même commit que le changement** : `README`, `docs/`,
  `CLAUDE.md`, glossaire de domaine (`UBIQUITOUS_LANGUAGE.md`), docstrings/commentaires. Si un
  commit change un comportement, une commande, une interface ou l'architecture, la doc
  correspondante est mise à jour *dans ce commit* — jamais « plus tard ». Une doc fausse est pire
  qu'une doc absente.

## Points d'arrêt humains
- Commit touchant l'**UI à valider visuellement** ou les **models / migrations** → rendre la
  main pour validation manuelle **avant** de committer.
