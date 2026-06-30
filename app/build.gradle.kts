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
//
// L'extraction est branchée **par variante** (cf. `androidComponents` plus bas) : chaque variante lit
// le jar H3 de son propre `RuntimeClasspath` et alimente son propre `jniLibs`. Un build debug ne
// dépend donc plus de la résolvabilité d'un classpath release ; si H3 était un jour scopé par
// variante, chaque APK embarquerait quand même ses `.so` (sinon : `UnsatisfiedLinkError` au runtime).
abstract class ExtractH3NativesTask
@javax.inject.Inject
constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultTask() {
    /** Jars H3 filtrés du `RuntimeClasspath` de la variante construite ; porteurs des `.so` natifs. */
    @get:Classpath
    abstract val h3Jars: ConfigurableFileCollection

    /** Dossier `jniLibs` généré, câblé par la variant API d'AGP au `jniLibs` de la variante. */
    @get:OutputDirectory
    abstract val jniLibsDir: DirectoryProperty

    @TaskAction
    fun extract() {
        fileSystemOperations.copy {
            from(h3Jars.files.map(archiveOperations::zipTree)) {
                ABI_BY_H3_PLATFORM.keys.forEach { include("$it/$H3_SO_NAME") }
                eachFile {
                    val abi = ABI_BY_H3_PLATFORM.getValue(path.substringBefore('/'))
                    relativePath = RelativePath(true, abi, H3_SO_NAME)
                }
                includeEmptyDirs = false
            }
            into(jniLibsDir)
        }
    }

    private companion object {
        /** Nom du `.so` H3, identique en source (dans le jar) et en sortie (dans `jniLibs`). */
        const val H3_SO_NAME = "libh3-java.so"

        /** Plateforme H3 (préfixe dans le jar) → ABI Android (dossier dans `jniLibs`). */
        val ABI_BY_H3_PLATFORM = mapOf("android-arm64" to "arm64-v8a", "android-arm" to "armeabi-v7a")
    }
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
        // Les tests UI Compose en saveur Robolectric lisent les ressources Android (chaînes, thème)
        // depuis src/test : sans cela, stringResource renverrait des valeurs vides hors émulateur.
        unitTests.isIncludeAndroidResources = true
        // Les tests unitaires de :app utilisent Kotest, qui s'exécute sur la plateforme JUnit 5
        // (comme les modules purs), plutôt que JUnit 4 par défaut d'AGP. junit-vintage-engine (ajouté
        // en testRuntimeOnly) y exécute en plus les tests Compose UI-test, restés en JUnit 4.
        unitTests.all { it.useJUnitPlatform() }
    }

    packaging {
        resources {
            // H3 embarque ses natifs desktop en ressources du classpath : inutiles sur Android (seuls
            // les `.so` extraits en jniLibs servent). On les exclut pour ne pas gonfler l'APK.
            excludes += setOf("darwin-*/**", "windows-*/**", "linux-*/**")
        }
    }
}

// Branche l'extraction des natifs H3 par variante : chaque variante enregistre sa propre tâche, lisant
// le jar H3 de son `RuntimeClasspath`, et l'expose à son `jniLibs` via la variant API (generated source
// directory — AGP câble la dépendance de tâche et le dossier de sortie). AGP n'exécute la tâche d'une
// variante que lorsque cette variante est construite : `assembleDebug` ne touche pas le classpath release.
androidComponents {
    onVariants { variant ->
        val variantName = variant.name
        val extractH3Natives =
            tasks.register<ExtractH3NativesTask>("extractH3Natives${variantName.replaceFirstChar(Char::uppercase)}") {
                h3Jars.from(
                    configurations.named("${variantName}RuntimeClasspath").map { classpath ->
                        classpath.files.filter { it.name.startsWith("h3-") && it.extension == "jar" }
                    },
                )
            }
        variant.sources.jniLibs?.addGeneratedSourceDirectory(extractH3Natives, ExtractH3NativesTask::jniLibsDir)
    }
}

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
    // Icônes vectorielles Material (set étendu) : glyphes du chrome UI (barre d'actions de la carte).
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Tests unitaires : Kotest (cohérent avec les modules purs) + temps virtuel des coroutines.
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Tests UI Compose en saveur Robolectric, dans src/test (pas d'androidTest instrumenté :
    // l'émulateur est fragile sur ce projet). Le harnais ui-test-junit4 est en JUnit 4 ; vintage le
    // fait tourner sous la plateforme JUnit 5 du projet. ui-test-manifest fournit le manifest de test
    // (debug) ; il pilote l'arbre sémantique, sans rendu visuel ni Mapbox/GPS.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testRuntimeOnly(libs.junit.vintage.engine)
}
