pluginManagement {
    // Plugins de convention partagés (modules Kotlin purs).
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Dépôt privé du SDK Mapbox : le téléchargement exige un token secret au scope
        // DOWNLOADS:READ. Lu depuis la propriété Gradle MAPBOX_DOWNLOADS_TOKEN
        // (~/.gradle/gradle.properties en local) ou la variable d'environnement homonyme (CI).
        // Voir README → Build & lancement → Prérequis.
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN")
                    .orElse(providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN"))
                    .orNull ?: ""
            }
            content { includeGroupByRegex("com\\.mapbox.*") }
        }
    }
}

rootProject.name = "Hexa"
include(":app")
include(":core")
include(":domain")
include(":location")
