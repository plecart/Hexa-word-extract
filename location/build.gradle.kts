// Module localisation & caméra : Kotlin pur, **zéro dépendance Android et zéro Mapbox**. Héberge la
// logique de poursuite à la troisième personne — contrôleur de caméra, lissage de cap, sources de
// position/orientation — sous forme de modules purs testables unitairement. L'intégration Mapbox
// (rendu, capteurs, ViewModel) vit dans `:app` et consomme ces contrats. Dépend de `:core` pour le
// type valeur géographique [com.hexa.core.geo.LatLng].
plugins {
    id("hexa.kotlin-pure-library")
}

dependencies {
    // Type valeur géographique partagé (LatLng) — exposé en `api` car il fait partie de la surface
    // publique de ce module (CameraState.center, PositionSource, ChaseCameraController.cameraFor) :
    // les consommateurs (`:app`) doivent pouvoir le nommer sans dépendre directement de `:core`.
    api(project(":core"))

    // Les sources de position/orientation s'expriment en Flow ; kotlinx-coroutines est pur JVM,
    // sans dépendance Android.
    implementation(libs.kotlinx.coroutines.core)

    // Temps virtuel (runTest) pour tester les sources Flow et le partage chaud sans attendre le temps réel.
    testImplementation(libs.kotlinx.coroutines.test)
}
