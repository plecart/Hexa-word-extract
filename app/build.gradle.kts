import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    // Traite google-services.json (non versionné) en ressources de configuration Firebase.
    alias(libs.plugins.google.services)
}

// Token public Mapbox, lu depuis local.properties (non versionné) avec repli sur la variable
// d'environnement (CI) puis chaîne vide — lint et tests unitaires compilent sans token réel.
val mapboxPublicToken: String =
    Properties().apply {
        val localProperties = rootProject.file("local.properties")
        if (localProperties.exists()) localProperties.inputStream().use(::load)
    }.getProperty("MAPBOX_PUBLIC_TOKEN")
        ?: System.getenv("MAPBOX_PUBLIC_TOKEN")
        ?: ""

android {
    namespace = "com.hexa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hexa"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"$mapboxPublicToken\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // BuildConfig expose le token public Mapbox (injecté depuis local.properties) et la
        // version de l'app.
        buildConfig = true
    }

    testOptions {
        // Les tests unitaires de :app utilisent Kotest, qui s'exécute sur la plateforme JUnit 5
        // (comme les modules purs), plutôt que JUnit 4 par défaut d'AGP.
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    // Modèles et configuration d'équilibrage, hébergés dans le module Kotlin pur :domain.
    implementation(project(":domain"))
    // Logique pure de poursuite caméra (contrôleur, lissage de cap, ports de position/cap).
    implementation(project(":location"))

    implementation(libs.mapbox.maps.android)
    implementation(libs.mapbox.maps.compose)

    // Firebase : compte anonyme (auth) et document joueur avec cache offline (firestore). La BoM
    // aligne les versions ; play-services adapte les Task Google en fonctions `suspend`.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // ViewModel + collecte d'état Compose conscient du cycle de vie (StateFlow → UI).
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Tests unitaires : Kotest (cohérent avec les modules purs) + temps virtuel des coroutines.
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
