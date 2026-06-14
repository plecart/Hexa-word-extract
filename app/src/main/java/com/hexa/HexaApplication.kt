package com.hexa

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Point d'entrée de l'application. Active le **cache offline persistant** de Firestore avant toute
 * lecture/écriture : l'app démarre et lit le document joueur sans réseau après un premier lancement
 * réussi (cf. PRD #5, user story 14). À configurer ici, une seule fois, avant le premier accès au
 * SDK.
 */
class HexaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
    }
}
