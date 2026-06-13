// Module domaine : Kotlin pur, **zéro dépendance Android**. La garantie est ici structurelle
// (vérifiée par le build), pas seulement par discipline : le SDK Android n'est pas sur le
// classpath. Héberge les modèles et la configuration d'équilibrage partagés par toute l'app.
plugins {
    id("hexa.kotlin-pure-library")
}

dependencies {
    // Utilitaires géométriques et de bruit purs (sphère, simplex 3D) consommés par le générateur.
    implementation(project(":core"))

    // H3 (lib native Uber) UNIQUEMENT en portée test : il alimente l'adaptateur de test qui
    // résout le centre d'une cellule et génère des indices variés. Le code de production de
    // :domain reste sans dépendance native — la résolution H3 réelle est injectée via le port
    // TileCenterLocator et câblée côté :app.
    testImplementation(libs.uber.h3)
}
