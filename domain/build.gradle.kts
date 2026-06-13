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

// Commande documentée de mesure empirique : exécute le test statistique sur un grand échantillon et
// imprime le rapport de distribution (présence mesurée vs cibles, seuils recalés proposés). Toujours
// ré-exécutée pour refléter l'état courant des constantes de GameConfig.
tasks.register<Test>("worldDistributionReport") {
    description = "Mesure la distribution du monde et imprime le rapport (mesuré vs cibles, seuils proposés)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("com.hexa.world.WorldDistributionStatisticalTest") }
    testLogging { showStandardStreams = true }
    outputs.upToDateWhen { false }
}
