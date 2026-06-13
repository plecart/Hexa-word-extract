// Module noyau : utilitaires Kotlin purs, génériques et sans sémantique métier (math, géométrie),
// **zéro dépendance Android**. Réutilisable côté serveur à terme. N'a aucune dépendance vers les
// autres modules — c'est la feuille du graphe (`core → rien`).
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
