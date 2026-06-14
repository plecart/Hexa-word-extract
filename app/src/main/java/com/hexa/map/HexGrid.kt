package com.hexa.map

import com.hexa.config.GameConfig
import com.hexa.core.geo.LatLng
import com.hexa.world.TileCenterLocator
import com.uber.h3core.H3Core

/**
 * Façade sur la grille hexagonale H3, frontière unique entre l'app et la bibliothèque native.
 *
 * Petite interface, implémentation riche : elle masque l'API H3 derrière les quatre seules opérations
 * dont l'app a besoin — localiser la cellule sous une position, énumérer le disque autour d'elle,
 * tracer un contour — et **résout le centre d'une cellule** en tant que [TileCenterLocator], pour que
 * le générateur de monde de `:domain` (Kotlin pur, sans natif) partage cette unique intégration.
 *
 * En isolant H3 ici, le reste de l'app reste mockable : un faux [HexGrid] suffit à tester la logique
 * de grille sans charger la bibliothèque native.
 */
interface HexGrid : TileCenterLocator {
    /** Index H3 de la cellule contenant [position]. */
    fun cellAt(position: LatLng): Long

    /**
     * Cellules du disque de `rings` anneaux autour de [center], centre inclus
     * (k anneaux → 1 + 3·k·(k+1) cellules : 2 → 19, 4 → 61).
     */
    fun disk(center: Long, rings: Int): List<Long>

    /** Sommets du contour de la cellule, dans l'ordre, pour en tracer le pourtour. */
    fun outline(cell: Long): List<LatLng>
}

/**
 * Adaptateur de production de [HexGrid] adossé à la bibliothèque native `com.uber:h3`.
 *
 * @param resolution résolution H3 des cellules, lue par défaut depuis la configuration centrale
 *   ([GameConfig.H3_RESOLUTION]) : changer l'échelle de la grille ne demande qu'un réglage.
 * @param h3 instance H3 native. Le chargement de la lib **diffère selon la plateforme** : sur
 *   **Android**, passer `H3Core.newSystemInstance()` (charge `libh3-java.so` depuis les `jniLibs` de
 *   l'APK, cf. `app/build.gradle.kts`) ; le défaut `H3Core.newInstance()` extrait la lib desktop
 *   empaquetée dans le jar et ne sert que sur JVM (tests). Sur Android, ce défaut échouerait
 *   (`UnsatisfiedLinkError`), AGP ne conservant pas les `.so` en ressources du classpath.
 */
class H3Grid(
    private val resolution: Int = GameConfig.H3_RESOLUTION,
    private val h3: H3Core = H3Core.newInstance(),
) : HexGrid {
    override fun cellAt(position: LatLng): Long = h3.latLngToCell(position.latDeg, position.lngDeg, resolution)

    override fun disk(center: Long, rings: Int): List<Long> = h3.gridDisk(center, rings)

    override fun outline(cell: Long): List<LatLng> =
        h3.cellToBoundary(cell).map { LatLng(latDeg = it.lat, lngDeg = it.lng) }

    override fun centerOf(h3Index: Long): LatLng =
        h3.cellToLatLng(h3Index).let { LatLng(latDeg = it.lat, lngDeg = it.lng) }
}
