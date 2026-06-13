package com.hexa.core.noise

import kotlin.math.floor
import kotlin.random.Random

/**
 * Bruit simplex 3D déterministe et seedé — fondation procédurale du générateur de monde.
 *
 * Pour un même [seed], `noise(x, y, z)` est une fonction **pure et reproductible sur toute JVM** :
 * la table de permutation est dérivée du seed par un générateur pseudo-aléatoire seedé
 * ([kotlin.random.Random]), sans dépendre d'un ordre d'itération ni d'un état global. Deux seeds
 * distincts produisent des champs indépendants.
 *
 * L'implémentation suit l'algorithme simplex de référence (Gustavson, *Simplex noise demystified*) :
 * plus rapide et moins sujet aux artefacts directionnels que le bruit de Perlin classique, et
 * échantillonnable en tout point de l'espace continu — ce qui permet de l'évaluer directement sur
 * la sphère (voir `com.hexa.core.geo.UnitSphere`) pour un monde sans couture ni distorsion polaire.
 *
 * Kotlin pur, aucune dépendance Android ni native : réutilisable côté serveur (anti-triche,
 * multijoueur post-MVP).
 *
 * @param seed graine du champ ; deux instances de même seed sont interchangeables.
 */
class SimplexNoise3D(seed: Long) {
    /** Table de permutation doublée pour indexer sans repli modulo à chaque accès. */
    private val perm: IntArray = buildPermutation(seed)

    /**
     * Échantillonne le bruit au point (x, y, z).
     *
     * @return une valeur dans l'intervalle [−1, 1], continue : deux points proches donnent des
     *   valeurs proches (pas de discontinuité).
     */
    fun noise(x: Double, y: Double, z: Double): Double {
        // Décale l'espace vers la grille simplexe (skew), puis identifie le coin de cellule unité.
        val skew = (x + y + z) * F3
        val i = floor(x + skew).toInt()
        val j = floor(y + skew).toInt()
        val k = floor(z + skew).toInt()

        val unskew = (i + j + k) * G3
        // Coordonnées du point relativement au coin de cellule, dans l'espace d'origine.
        val x0 = x - (i - unskew)
        val y0 = y - (j - unskew)
        val z0 = z - (k - unskew)

        // Les deux coins intermédiaires du tétraèdre (simplexe) contenant le point ; les coins
        // extrêmes sont toujours (0,0,0) et (1,1,1). Chaque coin est décalé d'un multiple de G3.
        val (mid1, mid2) = simplexCorners(x0, y0, z0)
        val sum =
            cornerContribution(x0, y0, z0, i, j, k, FIRST_CORNER, gShift = 0.0) +
                cornerContribution(x0, y0, z0, i, j, k, mid1, gShift = G3) +
                cornerContribution(x0, y0, z0, i, j, k, mid2, gShift = 2.0 * G3) +
                cornerContribution(x0, y0, z0, i, j, k, LAST_CORNER, gShift = 3.0 * G3)

        return SCALE * sum
    }

    /**
     * Contribution radiale d'un coin du simplexe : noyau d'atténuation `(R² − d²)⁴` pondéré par le
     * produit scalaire entre le gradient du coin et le vecteur qui l'en sépare. Nulle hors du rayon
     * d'influence [INFLUENCE_RADIUS_SQ].
     *
     * @param corner offset entier du coin par rapport au coin de base (i, j, k).
     * @param gShift décalage d'unskew cumulé du coin (multiple de [G3]).
     */
    private fun cornerContribution(
        x0: Double,
        y0: Double,
        z0: Double,
        i: Int,
        j: Int,
        k: Int,
        corner: Corner,
        gShift: Double,
    ): Double {
        val dx = x0 - corner.di + gShift
        val dy = y0 - corner.dj + gShift
        val dz = z0 - corner.dk + gShift

        var t = INFLUENCE_RADIUS_SQ - dx * dx - dy * dy - dz * dz
        if (t < 0.0) return 0.0
        t *= t

        val g = GRADIENTS[gradientIndex(i + corner.di, j + corner.dj, k + corner.dk)]
        return t * t * (g[0] * dx + g[1] * dy + g[2] * dz)
    }

    /** Indice de gradient (0..11) pour le coin (i, j, k), via la table de permutation. */
    private fun gradientIndex(i: Int, j: Int, k: Int): Int {
        val ii = i and (PERMUTATION_SIZE - 1)
        val jj = j and (PERMUTATION_SIZE - 1)
        val kk = k and (PERMUTATION_SIZE - 1)
        return perm[ii + perm[jj + perm[kk]]] % GRADIENTS.size
    }

    /** Offset entier d'un coin du simplexe par rapport au coin de base. */
    private data class Corner(val di: Int, val dj: Int, val dk: Int)

    private companion object {
        /** Facteur de skew (espace → grille simplexe) pour la dimension 3. */
        const val F3 = 1.0 / 3.0

        /** Facteur d'unskew (grille simplexe → espace) pour la dimension 3. */
        const val G3 = 1.0 / 6.0

        /** Rayon² d'influence d'un coin ; au-delà, sa contribution est nulle. */
        const val INFLUENCE_RADIUS_SQ = 0.6

        /** Normalise la somme des contributions vers l'intervalle [−1, 1]. */
        const val SCALE = 32.0

        /** Taille de la permutation de base (puissance de 2 → masquage par `and`). */
        const val PERMUTATION_SIZE = 256

        /** Coin de base du simplexe (offset nul) et coin opposé (offset (1,1,1)). */
        val FIRST_CORNER = Corner(0, 0, 0)
        val LAST_CORNER = Corner(1, 1, 1)

        /** Les 12 gradients 3D : milieux des arêtes du cube, convention simplex de référence. */
        val GRADIENTS: Array<IntArray> = arrayOf(
            intArrayOf(1, 1, 0), intArrayOf(-1, 1, 0), intArrayOf(1, -1, 0), intArrayOf(-1, -1, 0),
            intArrayOf(1, 0, 1), intArrayOf(-1, 0, 1), intArrayOf(1, 0, -1), intArrayOf(-1, 0, -1),
            intArrayOf(0, 1, 1), intArrayOf(0, -1, 1), intArrayOf(0, 1, -1), intArrayOf(0, -1, -1),
        )

        /**
         * Détermine les deux coins intermédiaires du simplexe contenant le point, par tri
         * décroissant de (x0, y0, z0) — l'axe dominant est franchi en premier.
         */
        fun simplexCorners(x0: Double, y0: Double, z0: Double): Pair<Corner, Corner> = when {
            x0 >= y0 && y0 >= z0 -> Corner(1, 0, 0) to Corner(1, 1, 0)
            x0 >= y0 && x0 >= z0 -> Corner(1, 0, 0) to Corner(1, 0, 1)
            x0 >= y0 -> Corner(0, 0, 1) to Corner(1, 0, 1)
            y0 < z0 -> Corner(0, 0, 1) to Corner(0, 1, 1)
            x0 < z0 -> Corner(0, 1, 0) to Corner(0, 1, 1)
            else -> Corner(0, 1, 0) to Corner(1, 1, 0)
        }

        /**
         * Construit une permutation de `0..PERMUTATION_SIZE-1` mélangée de façon déterministe par le
         * seed, puis la double pour que les accès `perm[a + perm[b + perm[c]]]` ne débordent jamais.
         */
        fun buildPermutation(seed: Long): IntArray {
            val base = (0 until PERMUTATION_SIZE).toMutableList()
            base.shuffle(Random(seed))
            return IntArray(PERMUTATION_SIZE * 2) { base[it and (PERMUTATION_SIZE - 1)] }
        }
    }
}
