---
name: plan-qa
description: À utiliser quand l'utilisateur veut planifier la QA d'un lot de travaux récemment implémentés — généralement après avoir fusionné des issues issues d'un PRD. Déclencheurs : « écris un plan de QA », « plan QA pour les issues #X #Y », « plan de QA pour le PRD », « je veux faire la QA de ce qu'on vient de livrer », ou après une session d'implémentation TDD.
---

# Rédacteur de plan de QA

Construis un plan de QA exhaustif et exécutable, cadré sur un lot de travaux précis, et publie-le sous forme d'issue GitHub avec un corps à cases à cocher. Le plan est ensuite exécuté par `execution-qa`, et les bugs trouvés pendant l'exécution sont déposés par `bug-vers-issue`.

## Principe fondamental

Le plan de QA doit être **cadré** (uniquement ce qui vient d'être livré), **exhaustif** (aucune hypothèse cachée) et **ancré dans la réalité** (vraies routes, vrais composants, vraies tables, vrais fixtures — pas inventés). Les checklists génériques sont un mauvais signe.

## Entrées (un mode parmi trois)

Mode 1 — **Référence PRD** : `/plan-qa #42` où `#42` est une issue PRD. Récupère le corps du PRD, puis `gh issue list --search "parent #42" --state all` (ou recherche les issues qui mentionnent `#42`) pour rassembler toutes les issues d'implémentation qui en découlent.

Mode 2 — **Liste explicite d'issues** : `/plan-qa #51 #52 #53`. Récupère le corps et les critères d'acceptation de chaque issue.

Mode 3 — **Aucun argument** : Recherche l'issue ouverte la plus récente portant le label `qa-plan`. Utilise `git log` depuis la date de création de cette issue pour détecter les commits référençant des issues d'implémentation. Si aucune issue `qa-plan` antérieure n'existe, demande à l'utilisateur quelles issues sont dans le périmètre plutôt que de deviner.

Dans tous les modes :
- Récupère le PRD s'il est connu. Sers-t'en pour la section « Vue d'ensemble ».
- Récupère le corps et les critères d'acceptation de chaque issue dans le périmètre.
- Lance `git log --grep="#<numéro-issue>"` pour chaque issue dans le périmètre afin de trouver les commits de livraison.
- Lis les diffs de ces commits (ou au moins les chemins de fichiers modifiés) pour que le plan s'ancre aux **vraies** routes, composants, tables, fichiers, variables d'environnement.

## Processus

### 1. Inspecte le dépôt

- Lis `CLAUDE.md`, `UBIQUITOUS_LANGUAGE.md` (ou glossaire équivalent), `docs/`, `README`, `Makefile`, `docker-compose*.yml`, le routeur/contrôleurs de l'API, les routes du frontend, le schéma/les migrations, et tout fixture de test touché par les commits dans le périmètre.
- Utilise `rg`/`rg --files` d'abord ; vérifie localement tout endpoint, commande ou chemin incertain avant de l'inclure comme instruction.
- Identifie le langage métier du projet. Le plan doit employer ces termes.

### 2. Décide où vit le plan

Essaie GitHub d'abord :
- Si `gh` est installé et que le cwd est un dépôt avec un remote GitHub, le plan est une **issue GitHub** portant le label `qa-plan` (crée le label s'il manque).
- Sinon, replie-toi sur un fichier markdown à `docs/qa/qa-plan-<slug>.md` (slug à partir du titre du PRD ou d'une courte étiquette de périmètre).

Indique à l'utilisateur quel mode tu utilises avant de rédiger.

### 3. Rédige le plan

Utilise `references/gabarit-plan-qa.md` comme squelette. Le plan a deux couches :

**Couche A — Phases transversales** (toujours incluses lorsque applicables) :
- Phase 0 — Préliminaires (env, build, démarrage, gates, clients générés)
- Phase 1 — Sécurité & Propriété (auth, isolation inter-locataires) — à inclure uniquement si l'application a de l'authentification
- Phase R — Régression (les flux existants fonctionnent toujours après la nouvelle livraison) — toujours inclure
- Phase Finale — Définition du Terminé

**Couche B — Blocs de fonctionnalité par issue**, intercalés entre les phases transversales dans l'ordre des dépendances. Un bloc par issue dans le périmètre. Utilise `references/exemple-bloc-issue.md` comme modèle. Chaque bloc inclut :

- Référence de l'issue (`#N — Titre`, lien)
- Référence au PRD source (le cas échéant)
- Critères d'acceptation (reformulés en cases `- [ ]`, copiés/normalisés depuis l'issue)
- Vérifications Backend / API — ancrées aux vraies routes et aux codes de statut réels
- Vérifications Frontend — ancrées aux vraies URLs/composants/états
- Vérifications BDD / stockage — ancrées aux vraies tables/chemins
- Vérifications Worker / logs — ancrées aux vrais services/flux de logs
- Commandes exactes ou actions UI
- Résultats attendus avant chaque action
- Règle d'arrêt
- Indices de sévérité pour les défaillances probables

Utilise `references/taxonomie-couverture.md` comme feuille d'indices de couverture — n'en tire que les sections pertinentes pour les fonctionnalités dans le périmètre. Ne colle pas la taxonomie entière.

### 4. Rends-le exhaustif dans le détail et la vue d'ensemble

Pour chaque bloc par issue, demande-toi :
- Ai-je couvert chaque critère d'acceptation par au moins une case à cocher ?
- Les surfaces backend, frontend, BDD, stockage, worker/logs sont-elles toutes considérées (à exclure explicitement avec « N/A » si une surface ne s'applique pas, sans l'omettre silencieusement) ?
- Les états d'erreur, états vides, états de chargement, états de succès sont-ils tous listés pour chaque surface UI ?
- Les variantes propriétaire/inter-locataires sont-elles testées pour toute ressource liée à un utilisateur ?
- Les jobs asynchrones sont-ils vérifiés comme atteignant un état terminal (succès et échec) ?
- Les uploads sont-ils vérifiés comme préservant les originaux et rejetant les fichiers invalides avant le stockage ?
- Les exports sont-ils vérifiés comme provenant de l'état persisté, et non de nouveaux appels IA régénérés ?

Pour le plan dans son ensemble :
- Y a-t-il une section **Vue d'ensemble** en haut indiquant : objectif du PRD, issues dans le périmètre, comportements visibles par l'utilisateur dans le périmètre, hors périmètre ?
- L'ordre des dépendances tient-il (sécurité avant les fonctionnalités qui en dépendent ; uploads avant extraction ; primitives backend avant le frontend qui les consomme) ?
- La **Définition du Terminé** est-elle explicite et binaire ?

### 5. Ancre chaque commande à une réalité vérifiée

- Utilise des variables d'environnement pour la répétabilité : `$QA_RUN`, `$TOKEN_A`, `$TOKEN_B`, `$CLIENT_ID`, etc.
- Requêtes BDD : uniquement après avoir vérifié les noms de table contre les migrations ou les modèles.
- Logs : bornés — `docker compose logs --tail=200 worker`, pas `--follow` non borné.
- Instructions frontend : inclus l'URL, le compte, l'état du navigateur (vider localStorage / préserver), les onglets DevTools.
- Les codes de statut, états terminaux des jobs et compteurs de lignes BDD font partie des « résultats attendus », pas des arrière-pensées.

### 6. Marque explicitement les décisions de politique

Quand un comportement dépend du produit ou est ambigu, marque la case avec `Décision QA :` et indique :
- Comportement actuel (d'après le code)
- Risque
- Politique recommandée
- Ce qui change si l'utilisateur choisit autrement

Le compagnon d'exécution enregistre le choix de l'utilisateur lorsqu'il y parvient.

### 7. Montre le brouillon à l'utilisateur, puis publie

- Affiche le corps complet du brouillon (ou un résumé plus le corps complet dans un bloc replié) et demande : « Publier ceci comme issue GitHub avec le label `qa-plan`, ou réviser ? »
- À l'approbation, publie :
  - GitHub : `gh issue create --label qa-plan --title "Plan QA: <périmètre>" --body-file <brouillon>`
  - Repli markdown : écris le fichier, affiche le chemin.
- Affiche l'URL de l'issue ou le chemin du fichier. Indique à l'utilisateur que c'est ce qu'il passe à `execution-qa` (ou que le compagnon découvrira automatiquement l'issue `qa-plan` la plus récente).

## Sortie : titre et labels de l'issue GitHub

- Titre : `Plan QA: <court périmètre>` — ex. `Plan QA: PRD #42 (base de connaissances v2)` ou `Plan QA: issues #51 #52 #53`.
- Labels : `qa-plan`. Crée le label s'il manque (`gh label create qa-plan --description "Checklist QA active" --color "0E8A16"`).
- Ne ferme AUCUNE issue source. Ne modifie PAS le PRD.

## Passe finale avant publication

Avant d'envoyer le corps à `gh issue create`, vérifie :

- [ ] La section **Vue d'ensemble** nomme le PRD, les issues dans le périmètre, les comportements dans le périmètre, les éléments hors périmètre.
- [ ] Chaque issue dans le périmètre a son propre bloc par issue.
- [ ] Chaque critère d'acceptation de chaque issue dans le périmètre apparaît comme au moins une case à cocher.
- [ ] Aucune case n'utilise une route, table, fichier, variable d'environnement ou fixture non vérifié contre le dépôt.
- [ ] Chaque job asynchrone a des vérifications d'état terminal (succès ET échec).
- [ ] Chaque surface d'upload/download a des vérifications de stockage et de ligne BDD.
- [ ] Chaque échec d'action utilisateur a une vérification de visibilité de l'erreur UI attendue.
- [ ] Chaque phase transversale a une règle d'arrêt.
- [ ] Les paliers de sévérité (bloquant / majeur / mineur) sont définis une fois en haut.
- [ ] Le gabarit de saisie d'échec (depuis `execution-qa/references/gabarit-intake-echec.md` ou en ligne) est inclus pour usage pendant l'exécution.
- [ ] La Définition du Terminé est binaire (`phases requises vertes, bloquants corrigés ou acceptés, ...`).
- [ ] Français, vocabulaire du domaine, pas de fuite de jargon interne.
- [ ] Aucun marqueur de remplissage (`TODO`, `<à compléter>`, etc.) ne subsiste.

## Erreurs fréquentes

| Erreur | Correction |
|---|---|
| Le plan couvre des zones qu'aucune issue récente n'a touchées | Recadrer. Uniquement les phases de couche A pour le transversal, les blocs de couche B liés aux issues dans le périmètre. |
| Vérifications génériques (« l'API marche », « l'UI s'affiche ») | Ancrer à des routes/composants/états/codes de statut attendus spécifiques. |
| Jobs asynchrones sans vérifications d'état terminal | Ajouter des chemins explicites `completed` ET `failed` avec les colonnes BDD attendues. |
| Uploads sans assertions BDD/stockage | Ajouter des vérifications de ligne (propriétaire, statut, finished_at) et des vérifications de chemin/taille de stockage. |
| Un unique état « réussi/échoué » par phase | Backend, frontend, BDD, logs sont suivis séparément — ne jamais les fusionner. |
| Longues queues de logs (`docker logs --follow`) | Utiliser des queues bornées. Les logs en preuve QA doivent être petits et copiables-collables. |
| Langues mélangées | Français, vocabulaire du domaine, pas de fuite de jargon interne. Les preuves collées par l'utilisateur restent dans leur forme native. |
