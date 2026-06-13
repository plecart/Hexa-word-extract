// Module domaine : Kotlin pur, **zéro dépendance Android**. La garantie est ici structurelle
// (vérifiée par le build), pas seulement par discipline : le SDK Android n'est pas sur le
// classpath. Héberge les modèles et la configuration d'équilibrage partagés par toute l'app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    // Kotest s'exécute sur la plateforme JUnit 5.
    useJUnitPlatform()
}
