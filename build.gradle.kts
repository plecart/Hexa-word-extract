// Build racine : déclare les plugins partagés sans les appliquer ici (`apply false`).
// Chaque module les active à la version figée dans `gradle/libs.versions.toml`,
// garantissant une chaîne d'outils homogène à mesure que le projet se découpe en modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}
