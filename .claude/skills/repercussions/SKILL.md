---
name: repercussions
description: À la clôture d'une issue (PR mergée), repasse sur toutes les autres issues ouvertes pour vérifier si la résolution — et la conversation qui l'a accompagnée — bouleverse la suite du projet : découpage, hypothèses, contrats d'interface, ordre des dépendances, doublons. Propose les corrections issue par issue et les applique sur permission. À utiliser juste après un merge qui ferme une issue, ou « vérifie l'impact de cette issue sur les autres ».
---

# Répercussions

Mouvement **sortant** de la pipeline, complémentaire de l'auto-challenge **entrant** de `cycle-pr`
(Étape 1, « le terrain a-t-il bougé depuis que l'issue a été écrite ? »).

Ici c'est l'inverse : une issue vient de se **fermer**. La PR mergée et la conversation qui l'a
résolue ont pu **acter des décisions, déplacer un contrat d'interface, déjà faire une partie d'un
autre lot, ou rendre une hypothèse caduque**. Le rôle de ce skill : confronter ce **delta réel** à
**toutes les autres issues ouvertes** et corriger celles que la suite ne tient plus.

**Règle d'or** : ce skill **propose**, il n'écrit jamais en silence. Aucune édition d'issue, aucun
commentaire, aucune fermeture sans **« go » explicite** du mainteneur. Au moindre doute, on
s'arrête et on demande.

Tout commentaire publié sur GitHub par ce skill commence par le disclaimer :

```
> *Généré par IA — analyse de répercussions à la clôture d'une issue.*
```

## Document de référence

- [references/grille-impact.md](references/grille-impact.md) — la taxonomie des types d'impact et
  la modification type pour chacun.

## Process

### 1. Capturer le « delta réel »

Avant de regarder les autres issues, écrire un résumé court de **ce que cette clôture a réellement
changé** — pas ce que l'issue promettait, ce qui a été *fait*. Sources :

- Le **diff mergé** (`git log` / `gh pr view <n> --json files,title,body`) — fichiers créés,
  déplacés, supprimés ; signatures, formats de données, noms publics modifiés.
- Les **décisions actées dans la conversation** qui a résolu l'issue (scope resserré ou élargi,
  hypothèse tranchée, choix d'archi, format figé, edge case décidé).
- Les **follow-ups différés / hors-scope** notés dans la PR (section `## Notes`).

Restituer sous un titre `## 📦 Delta réel` : une liste de faits courts et vérifiables. C'est le
seul matériau contre lequel les autres issues seront confrontées.

### 2. Constituer la liste des issues à réexaminer

Lire le **thème (milestone)** de l'issue fermée, puis (via `gh`, sur le dépôt de
`.claude/pipeline.config.md`) :

1. **D'abord, même thème** — `gh issue list --state open --milestone "<thème>"`. Ce sont les
   issues les plus probablement impactées. Examen **approfondi**.
2. **Puis, balayage large** — `gh issue list --state open` (autres milestones + sans milestone).
   Examen **léger** : on ne retient que les impacts évidents (contrat d'interface partagé, fichier
   déplacé qu'elles référencent, dépendance d'ordre, doublon).

Cas particuliers : issue fermée **sans thème**, ou merge d'une **demande directe** qui ne ferme
aucune issue → pas de cercle prioritaire, balayer directement toutes les issues ouvertes.

Annoncer les décomptes (« 4 issues dans le thème, 11 autres ouvertes »). Ne jamais tronquer en
silence : si le volume est gros et qu'on limite le balayage large, **le dire explicitement**.

### 3. Évaluer l'impact, issue par issue

Pour chaque issue retenue, lire corps + commentaires + brief d'agent éventuel, puis la confronter
au **delta réel** avec la grille ([references/grille-impact.md](references/grille-impact.md)) :

- **Hypothèse caduque** — l'issue suppose un état du code qui n'existe plus.
- **Contrat d'interface modifié** — signature / format / nom public sur lequel elle s'appuie a bougé.
- **Découpage à revoir** — la tranche n'a plus de sens, est partiellement faite, ou doit être
  re-découpée / fusionnée / scindée.
- **Ordre & dépendances** — l'issue dépendait de celle fermée (peut avancer ?) ou en débloque
  d'autres dans un ordre devenu faux.
- **Doublon / déjà fait** — la PR a déjà couvert tout ou partie de l'issue.
- **Conflit direct** — l'issue contredit une décision actée à la clôture.
- **Manque révélé** — la résolution a fait apparaître un besoin non couvert → nouvelle issue
  (déléguer à `vers-issues` ou `bug-vers-issue`).

Attribuer à chaque issue impactée un niveau : **bloquant** (la suite est fausse) / **à ajuster**
(reste valable moyennant correction) / **info** (signaler sans modifier).

### 4. Présenter le rapport — rien n'est encore écrit

Sous un titre `## 🔭 Répercussions`, lister **uniquement les issues impactées**, groupées par
niveau, et pour chacune :

```
#<num> — <titre>            [bloquant | à ajuster | info]
  Type d'impact : <de la grille>
  Pourquoi      : <le fait du delta réel qui la touche>
  Modif proposée: <édition du corps / commentaire / label / milestone / fermeture / nouvelle issue>
```

Si **aucune** issue n'est impactée, le dire explicitement : « Aucune répercussion : la clôture de
#X ne remet en cause aucune autre issue ouverte. » et s'arrêter là.

### 5. Appliquer — sur « go » explicite seulement

Après le « go » du mainteneur (global, ou issue par issue s'il préfère trier), appliquer les
modifications retenues. Toujours avec le disclaimer IA en tête de commentaire :

- **Édition du corps** — mettre à jour la spec / le brief d'agent ; en commentaire, résumer ce qui
  a changé et pourquoi (référence à #X et à la décision actée).
- **Commentaire seul** — quand on signale sans réécrire (niveau *info*, ou impact à confirmer par
  un humain).
- **Labels / état** — une issue dont l'hypothèse a sauté peut repasser `needs-triage` ou
  `needs-info` (déléguer la transition propre à `triage`).
- **Milestone** — rattacher à un autre thème si le delta a déplacé son sujet.
- **Fermeture en doublon** — seulement si l'impact est *déjà fait* et **non ambigu** ; commentaire
  expliquant le doublon + lien vers #X, puis fermer.
- **Nouvelle issue** — déléguer à `vers-issues` / `bug-vers-issue`, ne pas la rédiger ici.

Au moindre cas ambigu (re-découpage non trivial, conflit de conception, fermeture incertaine) :
**ne pas trancher seul**, remonter au mainteneur.

### 6. Récapituler

Lister ce qui a été fait : issues éditées / commentées, transitions d'état, fermetures, nouvelles
issues à créer en attente, et tout point laissé à l'arbitrage du mainteneur.

## Les 3 idées à retenir

1. On confronte les autres issues au **delta réel** (ce qui a été fait), jamais aux promesses de
   l'issue fermée.
2. Le skill **propose** ; il n'édite, ne ferme et ne re-trie qu'après un **« go » explicite**.
3. Priorité au **même thème**, balayage léger du reste — sans jamais masquer ce qu'on a écarté.
