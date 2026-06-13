import org.gradle.accessors.dm.LibrariesForLibs

// Plugin de convention : définition unique d'un module Kotlin pur du projet — **zéro dépendance
// Android** (toolchain JVM 17), formaté par ktlint, testé via Kotest sur JUnit 5. Tout module pur
// (`:core`, `:domain`, …) l'applique en une ligne plutôt que de dupliquer cette configuration.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

private val libs = the<LibrariesForLibs>()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    "testImplementation"(libs.kotest.runner.junit5)
    "testImplementation"(libs.kotest.assertions.core)
}

tasks.withType<Test>().configureEach {
    // Kotest s'exécute sur la plateforme JUnit 5.
    useJUnitPlatform()
}
