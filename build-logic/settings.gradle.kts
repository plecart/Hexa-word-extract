// Build composite isolé hébergeant les plugins de convention partagés par les modules du projet.
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    // Réutilise le version catalog racine : une seule source de vérité pour les versions.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
