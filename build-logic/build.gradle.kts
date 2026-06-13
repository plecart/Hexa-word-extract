plugins {
    `kotlin-dsl`
}

dependencies {
    // Plugins appliqués par les plugins de convention (versions issues du version catalog racine).
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    // Expose l'accesseur type-safe `libs` aux plugins de convention précompilés (limitation Gradle).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
