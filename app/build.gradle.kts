import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
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
}

dependencies {
    // Modèles et configuration d'équilibrage, hébergés dans le module Kotlin pur :domain.
    implementation(project(":domain"))

    implementation(libs.mapbox.maps.android)
    implementation(libs.mapbox.maps.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
