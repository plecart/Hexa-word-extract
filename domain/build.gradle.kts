// Module domaine : Kotlin pur, **zéro dépendance Android**. La garantie est ici structurelle
// (vérifiée par le build), pas seulement par discipline : le SDK Android n'est pas sur le
// classpath. Héberge les modèles et la configuration d'équilibrage partagés par toute l'app.
plugins {
    id("hexa.kotlin-pure-library")
}

dependencies {
    // Utilitaires géométriques et de bruit purs (sphère, simplex 3D) consommés par le générateur.
    implementation(project(":core"))

    // Coroutines : `Flow` est le type réactif exposé par le port joueur (`observe`). API (non
    // `implementation`) car `Flow` apparaît dans la signature publique du port — les consommateurs
    // (`:app`) en ont besoin sur leur classpath. Reste du Kotlin pur multiplateforme : `:domain`
    // demeure sans dépendance Android.
    api(libs.kotlinx.coroutines.core)

    // H3 (lib native Uber) UNIQUEMENT en portée test : il alimente l'adaptateur de test qui
    // résout le centre d'une cellule et génère des indices variés. Le code de production de
    // :domain reste sans dépendance native — la résolution H3 réelle est injectée via le port
    // TileCenterLocator et câblée côté :app.
    testImplementation(libs.uber.h3)

    // Temps virtuel (runTest) pour piloter les use cases `suspend` (amorçage joueur) en test. Le
    // code de production n'utilise que le mot-clé `suspend` (intrinsèque au compilateur) : aucune
    // dépendance coroutines n'est requise sur le classpath principal de ce module pur.
    testImplementation(libs.kotlinx.coroutines.test)
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
