package com.hexa.player

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Source **unique** des noms de collection et de la forme des chemins Firestore d'un joueur, partagée
 * par [FirestorePlayerRepository] et [FirestoreBuildingsRepository].
 *
 * La sous-collection des bâtiments [buildings] **dérive** du document joueur [player]
 * (`player(...).collection(...)`) plutôt que de recomposer le chemin à la main : un seul endroit
 * connaît le nom `players` et la forme `players/{uid}/buildings`. Un renommage de collection ou un
 * changement de hiérarchie ne touche donc qu'ici, et les deux repositories restent cohérents par
 * construction.
 *
 * Glue mince autour du SDK Firestore (comme les repositories qui la consomment) : non testée
 * unitairement — sa correction est verrouillée par les chemins eux-mêmes, inchangés.
 */
object FirestorePaths {
    private const val COLLECTION_PLAYERS = "players"
    private const val COLLECTION_BUILDINGS = "buildings"

    /** Document `players/{uid}` du joueur [id]. */
    fun player(firestore: FirebaseFirestore, id: PlayerId): DocumentReference =
        firestore.collection(COLLECTION_PLAYERS).document(id.value)

    /** Sous-collection `players/{uid}/buildings` du joueur [id], dérivée de son document [player]. */
    fun buildings(firestore: FirebaseFirestore, id: PlayerId): CollectionReference =
        player(firestore, id).collection(COLLECTION_BUILDINGS)
}
