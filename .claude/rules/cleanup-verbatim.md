# Règle — Le prompt de relecture (cleanup pass)

Partout où le travail demande une **relecture / cleanup pass / revue d'un delta de code**
(cycle de commit, auto-review de PR, refactor TDD, avant merge), exécuter **ce prompt mot pour
mot**. Ne jamais le paraphraser, ne jamais le raccourcir :

> Veuillez examiner l'ensemble du code ajouté et des modifications apportées aux fichiers
> existants. Réorganisez, structurez, optimisez et nettoyez ces modifications pour produire un
> code clair, cohérent et facilement maintenable. Éliminez tout code dupliqué, inutile ou
> obsolète. Décomposez les fonctions et séparez les responsabilités en suivant les principes
> KISS, DRY et YAGNI. L'objectif est d'obtenir un code propre, clair, concis et optimisé.

## Comment l'appliquer

- **Examen fichier par fichier**, pas un regard global vague. Pour chaque fichier modifié,
  lister les findings concrets : magic numbers, duplication, naming douteux, fonction trop
  longue, responsabilité mal séparée, code mort.
- **Rapporter à voix haute** sous un titre `## 🧹 Cleanup pass`, pour que le relecteur vérifie
  d'un coup d'œil que l'étape n'a pas été sautée.
- **Même rigueur sur les deltas triviaux.** Si « rien à corriger », l'expliquer ligne par ligne
  plutôt que de l'affirmer.
- Relancer les tests après le cleanup : il a pu casser quelque chose.
