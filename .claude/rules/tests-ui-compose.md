# Règle — Tests UI Compose (à chaque commit touchant un composable)

Le projet dispose d'une **infra de test UI Compose en JVM via Robolectric**, dans `src/test` (posée
par #75). Cette règle la rend d'usage **par défaut**, pas seulement « disponible si on y pense ».

## Ce qui est couvert obligatoirement
- **Tout composable ajouté ou modifié** dont le rendu dépend d'un état (binding état → UI, contenu
  conditionnel, présence/absence d'un élément, libellé/`contentDescription` d'une action) est
  couvert par un **test UI Compose** en `src/test`. **Aucun composable à logique d'affichage non
  testé n'entre dans l'historique** (corollaire de « aucun commit cassé », cf. `contraintes.md`).
- Reste validé par `@Preview` + œil humain, **sans test** : le rendu purement esthétique (couleurs,
  espacements, typographie de la DA). On ne teste pas un pixel ; on teste un **comportement
  observable** sur l'arbre sémantique.

## Comment (conventions établies, à réutiliser — ne pas reconfigurer)
- **Saveur Robolectric en JVM**, `src/test`. **Jamais d'`androidTest` instrumenté** (émulateur
  fragile sur ce projet : H3 x86, Mapbox `SurfaceView`, boussole figée).
- La **cohabitation JUnit 4 (Compose UI-test) ↔ JUnit 5 (Kotest)** est déjà en place via
  `junit-vintage-engine` : on l'utilise telle quelle, on ne retouche pas le harnais.
- `@RunWith(RobolectricTestRunner::class)` + `@Config(application = Application::class)` — une
  `Application` nue, pour ne pas tirer Firebase/H3/GPS dans un test de composable isolé.
- `createComposeRule()` + `HexaTheme { … }` ; libellés attendus **résolus via `Context`**
  (`ApplicationProvider`), jamais copiés en dur.
- Assertions sur l'**arbre sémantique** (`onNodeWithText` / `onNodeWithContentDescription` +
  `assertIsDisplayed` / `assertDoesNotExist`) — pas de rendu visuel, pas de Mapbox/GPS.
- Si un écran/`ModalBottomSheet` est difficile à piloter, **extraire un composable de contenu**
  rendable directement (modèle `TileInspectionContent` extrait de `TileInspectionSheet`), plutôt que
  de composer la coquille complète.

## Références
- Le « comment » détaillé (exemple runnable) : skill `kotlin-testing`, section *Compose UI Testing*.
- Mise en place de l'infra : `README.md` (« tests d'UI Compose de `:app` »).
- Tranche fondatrice : #75. Élargissement de la couverture aux écrans : #66.
