package com.hexa.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext

/**
 * Décode une image bitmap du dossier `assets/` en [Painter] Compose, mémorisé tant que le chemin ne
 * change pas. Pont minimal entre [android.content.res.AssetManager] et l'API de dessin Compose, pour
 * afficher les `icon.png` de la convention d'assets (cf. [ObjectAssets]) sans dépendance externe.
 *
 * Le décodage (lecture + `BitmapFactory`) ne s'exécute qu'au premier passage ou à un changement de
 * [path] ; les recompositions suivantes réutilisent le bitmap. Réservé aux assets **embarqués** : un
 * chemin absent lève une `IOException` au décodage (erreur de convention, pas d'entrée utilisateur).
 *
 * @param path chemin de l'image relatif à `assets/` (ex. `objects/cendrite/icon.png`).
 * @return un [Painter] prêt à passer à `Image`.
 */
@Composable
fun rememberAssetPainter(path: String): Painter {
    val assets = LocalContext.current.assets
    val bitmap = remember(path) {
        assets.open(path).use(BitmapFactory::decodeStream).asImageBitmap()
    }
    return remember(bitmap) { BitmapPainter(bitmap) }
}
