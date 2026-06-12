# Gabarit de plan de QA

Voici le squelette par défaut du corps d'issue GitHub pour un plan de QA. Remplace les espaces réservés. Supprime les sections qui ne s'appliquent pas (et dis-le explicitement avec `N/A : <raison>` plutôt que de les omettre silencieusement).

```markdown
# QA Plan: <court périmètre>

## Vue d'ensemble

- **PRD** : #<issue-prd> — <titre du PRD>
- **Issues dans le périmètre** : #<a>, #<b>, #<c>
- **Comportements visibles par l'utilisateur dans le périmètre** :
  - <une puce par comportement — langage clair, termes du domaine>
- **Hors périmètre** : <puces>
- **Déploiement / état des données supposé** : <ex. BDD vierge, fixtures X/Y seedés, feature flag Z activé>

## Paliers de sévérité

- **Bloquant** : fuite de sécurité, accès inter-locataires, perte de données/source, upload dangereux accepté, workflow central impossible, crash API/worker, export inutilisable.
- **Majeur** : mauvais comportement métier, gate manquant, erreur backend masquée dans l'UI, contexte périmé, job échoué sans erreur exploitable, polling qui ne s'arrête jamais.
- **Mineur** : formulation, finition visuelle, état vide non bloquant, message peu clair mais récupérable.

## Saisie d'échec (à utiliser pendant l'exécution)

Quand une case échoue, capture ce bloc dans la conversation. `bug-vers-issue` le consomme.

```text
Bloc QA :
Issue(s) :
Action :
Attendu :
Observé :
URL / endpoint :
Charge utile (si API) :
Console frontend :
Réponse réseau :
Logs API :
Logs worker :
Lignes BDD :
Stockage / artefacts :
Décision / politique :
```

## Données de test

- **Fixtures principaux** : <liste avec chemins vérifiés contre le dépôt>
- **Fixtures alternatifs** : <liste>
- **Fixtures invalides** : <liste>
- **Attentes du cas de référence** : <le cas échéant>

---

## Phase 0 — Préliminaires

**Objectif** : Prouver que l'environnement local/dev, les gates de qualité, le démarrage de l'app, les clients générés et les contrôles de sûreté production sont utilisables. Ne commence pas la QA des fonctionnalités si cette phase est rouge.

**Checklist** :
- [ ] Les fichiers d'env existent et correspondent à `.env.example`.
- [ ] Le gate de qualité backend passe — `<commande>`.
- [ ] Les tests frontend passent — `<commande>`.
- [ ] Le build frontend passe — `<commande>`.
- [ ] Docker Compose démarre les services requis — `<commande>`.
- [ ] La santé de l'API est disponible à `<url>`.
- [ ] L'URL frontend est disponible à `<url>`.
- [ ] OpenAPI / client généré à jour — `<commande>`.
- [ ] La config façon production refuse les valeurs par défaut non sûres.

**Règle d'arrêt** : Ne commence pas la QA des fonctionnalités si l'app ne démarre pas ou si les gates stricts sont rouges.

---

## Phase 1 — Sécurité & Propriété

(À inclure uniquement si l'app a de l'authentification et des ressources liées à un utilisateur. Sinon, marque `N/A : <raison>`.)

**Objectif** : Prouver que les utilisateurs ne peuvent ni voir ni modifier les ressources des autres.

**Préconditions** :
- L'utilisateur A et l'utilisateur B existent (utilise `$USER_A_TOKEN`, `$USER_B_TOKEN`).
- L'utilisateur A possède au moins une ressource racine et des enfants imbriqués.

**Backend / API** :
- [ ] Token manquant → 401.
- [ ] Token invalide / expiré → 401.
- [ ] L'utilisateur A liste ses propres ressources → compte attendu.
- [ ] L'utilisateur B ne peut pas lister / lire / modifier / télécharger / générer / exporter les ressources de l'utilisateur A → 403 ou 404 selon la politique anti-énumération.
- [ ] Aucune route ne retourne 500 pour un refus d'accès.

**Frontend** :
- [ ] Aucun token → surface de connexion uniquement.
- [ ] Connexion valide → espace de travail.
- [ ] Token invalide / expiré → session vidée, redirection vers la connexion.
- [ ] Le rafraîchissement préserve uniquement le contexte voulu (aucune fuite de parent périmé).

**BDD / logs** :
- [ ] Les clés étrangères de propriété sont correctes sur chaque ligne de test.
- [ ] Aucune ligne inter-locataires créée pendant les actions rejetées.

**Règle d'arrêt** : Toute donnée inter-locataires visible est bloquante.

---

<!-- Les blocs de fonctionnalité par issue vont ici, dans l'ordre des dépendances. Voir exemple-bloc-issue.md -->

## Bloc de fonctionnalité : Issue #<N> — <Titre court>

**Source** : #<N> | PRD #<P> | commits de livraison : `<sha1>`, `<sha2>`

**Objectif** : <reformule le comportement visible par l'utilisateur en une phrase>

**Critères d'acceptation** (depuis #<N>) :
- [ ] <critère 1>
- [ ] <critère 2>

**Vérifications Backend / API** :
- [ ] `<MÉTHODE> <route>` avec entrée valide → `<statut attendu>`, le corps correspond à `<extrait-de-schéma>`.
- [ ] `<MÉTHODE> <route>` avec entrée invalide → `<statut attendu>`, erreur visible.
- [ ] Les variantes propriétaire / inter-locataires se comportent correctement.
- [ ] Le job asynchrone (le cas échéant) atteint `completed` ET, sur une variante d'échec, atteint `failed` avec une erreur exploitable.

**Vérifications Frontend** :
- [ ] URL / surface : `<url>` depuis le compte `<compte>`.
- [ ] L'état vide s'affiche.
- [ ] L'état de chargement s'affiche pendant l'action en attente.
- [ ] L'état de succès s'affiche avec les données du backend.
- [ ] L'état d'erreur affiche le détail du backend (pas un wrapper générique).
- [ ] Les boutons sont désactivés pendant l'attente.
- [ ] Le polling s'arrête à l'état terminal (le cas échéant).
- [ ] Console propre / Réseau propre.

**Vérifications BDD / stockage** :
- [ ] Ligne créée dans `<table>` avec les colonnes attendues et la FK de propriétaire.
- [ ] Originaux préservés à `<chemin de stockage>` (si upload).
- [ ] Colonnes `finished_at`, `error` correctement renseignées aux états terminaux.

**Vérifications Worker / logs** :
- [ ] Les logs du worker ne montrent aucune exception non gérée pendant le chemin nominal.
- [ ] La variante d'échec logge une erreur exploitable, sans boucle de crash.

**Règle d'arrêt** : <quand s'arrêter et trier avant de continuer>

---

<!-- D'autres blocs de fonctionnalité ici -->

---

## Phase R — Régression

**Objectif** : Les flux existants fonctionnent toujours après la nouvelle livraison.

**Checklist** :
- [ ] <fonctionnalité antérieure 1> passe toujours son chemin de référence — `<commande ou UI>`.
- [ ] <fonctionnalité antérieure 2> passe toujours — `<commande ou UI>`.
- [ ] La suite de tests automatisés existante est verte — `<commande>`.
- [ ] Aucun nouveau bruit de console / réseau / logs sur les surfaces antérieures.

**Règle d'arrêt** : Une régression dans un comportement antérieurement livré est au moins majeure.

---

## Phase Finale — Définition du Terminé

- [ ] Chaque case de la Phase 0 est verte.
- [ ] Chaque case de la Phase 1 est verte ou explicitement N/A.
- [ ] Chaque bloc de fonctionnalité par issue est entièrement coché.
- [ ] Chaque case de la Phase R est verte.
- [ ] Chaque constat bloquant est corrigé ou explicitement accepté par l'utilisateur, avec la décision consignée en ligne.
- [ ] Chaque constat majeur a un lien `🔴 BUG #N` ou une décision `reporté` explicite.
- [ ] Commandes finales relancées après corrections — `<commandes>`.

---

## Index des constats

(execution-qa ajoute des liens ici à mesure que les bugs sont déposés via bug-vers-issue.)

- 🔴 #<numéro-issue-bug> — <titre court> — phase / bloc : `<emplacement>`
```
