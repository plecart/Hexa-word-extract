import org.gradle.api.file.RelativePath
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

// La lib native H3 (`com.uber:h3`) embarque ses `.so` comme **ressources du classpath** par
// plateforme (`android-arm64/…`, `android-arm/…`), pas sous `lib/<abi>/`. AGP les écarte donc du
// packaging, et `H3Core.newInstance()` (qui lit la lib en ressource) échoue sur Android. On extrait
// les `.so` Android du jar vers un dossier `jniLibs` généré : ils sont alors packagés comme libs
// natives standard et chargeables via `System.loadLibrary` (`H3Core.newSystemInstance()`).
val h3JniLibsDir = layout.buildDirectory.dir("generated/h3-jniLibs")
val abiByH3Platform = mapOf("android-arm64" to "arm64-v8a", "android-arm" to "armeabi-v7a")
val extractH3Natives by tasks.registering(Copy::class) {
    val h3Jars = configurations.named("releaseRuntimeClasspath").map { classpath ->
        classpath.files.filter { it.name.startsWith("h3-") && it.extension == "jar" }
    }
    from(h3Jars.map { jars -> jars.map(::zipTree) }) {
        abiByH3Platform.keys.forEach { include("$it/libh3-java.so") }
        eachFile {
            val abi = abiByH3Platform.getValue(path.substringBefore('/'))
            relativePath = RelativePath(true, abi, "libh3-java.so")
        }
        includeEmptyDirs = false
    }
    into(h3JniLibsDir)
}

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

    // Les `.so` H3 extraits (cf. extractH3Natives) sont packagés comme libs natives de l'APK.
    sourceSets.getByName("main").jniLibs.srcDir(h3JniLibsDir)

    packaging {
        resources {
            // H3 embarque ses natifs desktop en ressources du classpath : inutiles sur Android (seuls
            // les `.so` extraits en jniLibs servent). On les exclut pour ne pas gonfler l'APK.
            excludes += setOf("darwin-*/**", "windows-*/**", "linux-*/**")
        }
    }
}

// L'extraction des natifs H3 doit précéder le packaging des jniLibs.
tasks.named("preBuild").configure { dependsOn(extractH3Natives) }

dependencies {
    // Modèles et configuration d'équilibrage, hébergés dans le module Kotlin pur :domain.
    implementation(project(":domain"))
    // Logique pure de poursuite caméra (contrôleur, lissage de cap, ports de position/cap).
    implementation(project(":location"))

    implementation(libs.mapbox.maps.android)
    implementation(libs.mapbox.maps.compose)

    // Grille hexagonale H3 (lib native Uber) : seule intégration H3 de production. Elle sert le rendu
    // de la grille ET résout le centre des cellules pour le générateur de monde (port TileCenterLocator
    // de :domain, où H3 reste cantonné aux tests).
    implementation(libs.uber.h3)

    // Firebase : compte anonyme (auth) et document joueur avec cache offline (firestore). La BoM
    // aligne les versions ; play-services adapte les Task Google en fonctions `suspend`.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    // Position GPS réelle via FusedLocationProviderClient (Google Play services).
    implementation(libs.play.services.location)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // ViewModel + collecte d'état Compose conscient du cycle de vie (StateFlow → UI).
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Tests unitaires : Kotest (cohérent avec les modules purs) + temps virtuel des coroutines.
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
