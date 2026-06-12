# Taxonomie de couverture

Une feuille d'indices de couverture pour `plan-qa`. N'en tire que les sections pertinentes pour le travail dans le périmètre — ne colle pas la taxonomie complète dans un plan.

## Environnement

- Les fichiers d'env requis existent et correspondent à `.env.example`.
- Le gate de qualité backend passe.
- Les tests et le build frontend passent.
- L'orchestrateur de conteneurs / processus démarre tous les services.
- La santé, OpenAPI, le client généré et l'URL frontend sont joignables.
- La config façon production refuse les valeurs par défaut non sûres (debug off, cookies signés, clés secrètes non par défaut).

## Sécurité & Propriété

- Token manquant → 401.
- Token invalide / expiré → 401 et session frontend vidée.
- L'utilisateur A ne voit que les ressources de l'utilisateur A sur chaque endpoint de liste.
- L'utilisateur B ne peut pas lister, lire, modifier, générer, exporter ou télécharger les ressources de l'utilisateur A.
- Les ressources manquantes et inter-locataires suivent la convention anti-énumération du projet (cohérence 403 vs 404).
- Le frontend ne persiste que les clés de contexte sûres ; les sélections parent / enfant périmées sont réconciliées après rafraîchissement ou changement de contexte.

## Upload & Extraction

- Formats valides acceptés.
- Magic bytes invalides / extension non concordante rejetés avant le stockage.
- Fichiers vides et trop volumineux rejetés.
- La politique des fichiers sans extension est testée et consignée.
- Les formats hérités non supportés échouent de façon contrôlée.
- Les fichiers originaux sont préservés et téléchargeables (avec auth) lorsqu'ils sont exposés.
- Les fichiers rejetés ne créent ni ligne BDD ni artefact de stockage exploitable.
- Les jobs d'extraction asynchrones atteignent un statut terminal (`completed` ou `failed`).
- Les messages d'erreur sont visibles dans l'API et le frontend.
- Les lignes BDD et chemins de stockage respectent les attentes de propriété et de cycle de vie.
- La sémantique de reprise / idempotence est testée pour les tentatives d'extraction répétées.
- Les logs du worker ne montrent aucune exception non gérée pour les échecs contrôlés.

## Couverture des états du frontend

Pour chaque surface UI qui touche des données backend, toutes celles-ci :

- État vide.
- État de chargement (avec contrôles désactivés).
- État de succès.
- État d'erreur (détail backend visible, non remplacé par un wrapper générique).
- État terminal d'échec (pour les jobs asynchrones — polling arrêté, erreur affichée sur la ligne concernée).

Plus :

- Le rafraîchissement préserve uniquement le contexte voulu.
- Les liens / téléchargements fonctionnent avec le modèle d'auth de l'app (URL signée, cookie, header).
- Les sélections parent / enfant réconciliées après rechargement ou changement de contexte.
- La console n'a aucune erreur inexpliquée.
- Le réseau n'a aucune requête échouée inexpliquée.

## Intégrité des données

- Les clés étrangères BDD pointent vers le bon périmètre de propriétaire.
- Les lignes créées ont les statuts et horodatages attendus.
- Les lignes dérivées conservent les liens / citations source lorsque applicable.
- Les jobs échoués conservent une `error` claire et ne laissent pas d'états `running` périmés.
- Les règles de doublon / reprise / idempotence sont testées.
- Les objets de stockage existent pour les uploads acceptés.
- Les uploads rejetés ne laissent aucun artefact exploitable.
- Les jobs terminaux ont `finished_at` (ou l'équivalent du projet).

## Workflow métier

- Les objets du domaine sont persistés structurellement, pas seulement comme des blobs.
- Les gates sont appliqués par le backend ; le frontend affiche les blocages du backend.
- Les décisions utilisateur requises sont explicites et auditables.
- Les exports utilisent des objets structurés persistés, pas un état caché régénéré.

## Contrat d'API

- Les codes de statut correspondent au contrat documenté pour les chemins nominaux et d'erreur.
- Les corps correspondent au schéma OpenAPI / du client généré.
- Pagination, filtrage, tri sont stables d'un appel à l'autre.
- La différenciation 422 / 400 est cohérente.

## Jobs / Workers / Async

- Les exceptions du worker ne font pas crasher la boucle du worker.
- Les erreurs de fournisseur / réseau créent des lignes d'audit ou des champs d'erreur persistés.
- Les réponses vides / nulles / invalides de l'amont échouent clairement.
- Les tests automatisés utilisent des mocks plutôt que des fournisseurs en direct.
- Les reprises de job sont bornées et observables.

## Régression

- Le plan nomme les commandes minimales à relancer après corrections.
- Les voies d'avertissement connues sont séparées des gates bloquants.
- Les cas de référence utilisent des assertions qui évitent les comparaisons fragiles de longs textes.
- Les corrections de bug incluent des tests ciblés avant que les gates plus larges ne soient relancés.
- Le corps du plan consigne l'échec original et la vérification après correction (via l'issue de bug liée).

## Hygiène

- Les migrations fonctionnent sur une BDD vierge.
- L'app démarre sans cycles d'import.
- Les gates de type / lint / format passent.
- Les commentaires expliquent les invariants durables, pas l'historique de livraison.
