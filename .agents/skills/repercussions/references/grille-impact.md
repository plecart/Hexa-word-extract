# Grille d'impact

Les sept types de répercussion qu'une issue fermée peut avoir sur une autre issue ouverte. Pour
chacun : le **signal** qui le révèle (en confrontant l'issue au *delta réel*) et la **modification
type** à proposer. Une même issue peut cumuler plusieurs types.

| Type | Signal (dans le delta réel) | Modification type proposée |
|---|---|---|
| **Hypothèse caduque** | L'issue décrit un état du code (module, flux, donnée) que la PR a changé. | Éditer le corps pour refléter l'état actuel ; si l'hypothèse était structurante → `needs-triage`. |
| **Contrat d'interface modifié** | Signature, format de payload, nom public, chemin de fichier que l'issue référence a bougé. | Mettre à jour les références dans le brief d'agent ; commenter le nouveau contrat + lien vers #X. |
| **Découpage à revoir** | La tranche est partiellement faite, vidée de son sens, ou trop grosse/petite face au nouveau périmètre. | Re-spécifier le scope ; proposer fusion/scission (sans trancher seul si non trivial → mainteneur). |
| **Ordre & dépendances** | L'issue dépendait de celle fermée, ou en débloque d'autres dont l'ordre est maintenant faux. | Lever un blocage (`ready-for-agent`), ou réordonner ; noter la dépendance dans le corps. |
| **Doublon / déjà fait** | La PR couvre tout ou partie de l'issue. | Si *tout* et non ambigu → fermer en doublon (commentaire + lien). Si *partie* → réduire le scope. |
| **Conflit direct** | L'issue demande l'inverse d'une décision actée à la clôture. | Commenter le conflit, remonter au mainteneur ; candidate à `wontfix` ou re-spec. |
| **Manque révélé** | La résolution a exposé un besoin non couvert par aucune issue. | Déléguer la création à `vers-issues` / `bug-vers-issue` ; ne pas rédiger l'issue ici. |

## Niveaux

- **Bloquant** — sans correction, la suite du dev partirait sur une base fausse (hypothèse
  structurante cassée, contrat rompu, conflit direct). À traiter avant de reprendre le thème.
- **À ajuster** — l'issue reste valable moyennant une mise à jour (référence, scope, ordre).
- **Info** — à signaler pour traçabilité, sans modifier la spec.

## Garde-fous

- Confronter au **fait** du delta réel, pas à une intuition. Si on ne peut pas pointer le fait qui
  impacte l'issue, ce n'est pas une répercussion — ne pas l'inventer.
- Une **fermeture en doublon** exige un recouvrement *total et non ambigu*. Dans le doute : scope
  réduit + commentaire, jamais fermeture.
- Tout **re-découpage non trivial** ou **conflit de conception** se remonte au mainteneur ; ce
  skill ne tranche pas la conception à sa place.
