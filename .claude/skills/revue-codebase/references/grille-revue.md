# Grille de revue — les 10 axes

Dérouler ces axes **pour chaque fichier**, puis à l'échelle de chaque module. Chaque point est une
question à se poser ; un finding = une réponse négative **étayée par une preuve** (usage réel,
duplication montrée, ligne précise). La sévérité entre crochets est indicative — à ajuster au
contexte. La source de la règle est notée après `·`.

## A. Architecture & frontières · contraintes.md (modulaire/scalable) ; modules-profonds.md
1. Le graphe de dépendances est-il acyclique et bien orienté (modules purs ← Android/SDK) ? [🔴 si fuite]
2. Les frontières natives/externes (H3, Mapbox, Firebase, GPS) sont-elles isolées derrière des ports ? [🔴/🟡]
3. Modules profonds (petite interface, implémentation riche) ? Repérer les modules superficiels qui passent-plat. [🟡]
4. Couplage fort, dépendances qui contournent une interface, responsabilités éclatées entre fichiers ? [🟡]
5. Chaque fichier est-il dans le bon package/module ? Repérer les packages fourre-tout. [🟢/🟡]

## B. Décomposition & responsabilité unique · verbatim ; taille-pr.md ; refactoring.md
6. Fonctions trop longues / fourre-tout à découper en helpers privés ? [🟡]
7. Fichiers monolithiques à fractionner en unités ciblées testables isolément ? [🟡]
8. Responsabilité unique par classe/fonction ? Feature envy (logique loin de ses données) ? [🟡]
9. Obsession des primitifs : tuples / `Double` anonymes là où un value object clarifierait ? [🟢]

## C. Duplication / DRY · verbatim ; cycle-pr auto-review
10. Blocs ou helpers dupliqués à extraire ? [🟡]
11. Constantes / magic numbers répétés à travers fichiers ou modules ? [🟡]
12. Couplage par convention : valeurs qui *doivent* rester synchrones sans garantie compilateur ? [🟡]

## D. YAGNI / code mort & obsolète · contraintes.md (pas de scaffold ni placeholder)
13. Symboles déclarés et jamais consommés en production (vérifier les usages) ? [🔴]
14. Scaffold « au cas où » / fichiers placeholder ? [🔴/🟡]
15. Wrappers prématurés (abstraction d'un seul champ sans usage) — accepter si couture documentée. [🟢]
16. Imports wildcard, `print()`/log de debug, `TODO`/`FIXME` sans propriétaire ni explication ? [🟡]

## E. Clarté & cohérence · verbatim
17. Nommage douteux/incohérent ; respect du glossaire métier et des ADR de la zone ? [🟡]
18. Idiome & style homogènes avec le voisinage (densité de commentaires, conventions) ? [🟢]
19. Conventions de commit/branche observables dans l'historique (mention, pas blocage) ? [🟢]

## F. Documentation synchronisée · contraintes.md (doc à jour dans le même commit)
20. KDoc/commentaires décrivant un comportement révolu (doc qui ment) ? [🟡]
21. Références d'issues périmées (`#N` renvoyant à une tranche déjà livrée) ? [🟡]
22. README / CLAUDE.md / docs cohérents avec commandes, interfaces et architecture actuelles ? [🟡]
23. Fonctions publiques documentées (contrat, params, erreurs, edge cases) ? [🟢]

## G. Scalabilité · contraintes.md (scalable)
24. Ajouter un cas se fait-il par extension (nouvelle implémentation) ou par modification invasive ? [🟡]
25. Hypothèses qui ne tiennent qu'à petite échelle : N+1, état global, limites en dur non justifiées ? [🟡]
26. Couplage positionnel fragile (listes indexées par `ordinal` d'enum, cassable en silence) ? [🟡]

## H. Robustesse & correctness · signalement, pas chasse aux bugs → route vers bug-vers-issue
27. Asymétries de tolérance (cast dur au milieu d'un mapper par ailleurs tolérant) ? [🔴/🟡]
28. Gestion des erreurs / edge cases / race conditions visibles à la lecture (flux, coroutines, null) ? [🔴/🟡]
29. Garde-fous compile-time présents là où ils protègent un invariant (`when` exhaustif, `require`) ? [🟢]

## I. Tests & vérifiabilité · cycle-pr ; refactoring.md
30. Logique pure extraite et testée isolément ; ViewModels/adaptateurs en glu mince ? [🟡]
31. Zones de complexité non couvertes par des tests (signalement) ? [🟡]
32. Dépendances injectées, frontières mockables (testabilité) ? [🟢]

## J. Forme du rapport · cleanup-verbatim.md (rapporter à voix haute)
33. Examen fichier par fichier, findings concrets — pas de survol vague.
34. Verdict par sévérité 🔴/🟡/🟢 avec références `fichier:ligne` cliquables.
35. Section « ce qui est bien » à préserver.
36. Recommandations groupées en lots / PR potentielles, sans rien modifier.
37. Rappel explicite : aucune modification de code effectuée.
