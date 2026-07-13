import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use(localProperties::load)
}

android {
    namespace = "com.example.newsapp"
    compileSdk = 36

    composeCompiler {
        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
    }

    defaultConfig {
        applicationId = "com.example.newsapp"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val newsApiKey = (
            providers.gradleProperty("NEWS_API_KEY").orNull
                ?: localProperties.getProperty("NEWS_API_KEY")
                ?: System.getenv("NEWS_API_KEY")
                ?: ""
            ).trim()
        buildConfigField("String", "NEWS_API_KEY", "\"$newsApiKey\"")
        
        val webClientId = (
            providers.gradleProperty("WEB_CLIENT_ID").orNull
                ?: localProperties.getProperty("WEB_CLIENT_ID")
                ?: System.getenv("WEB_CLIENT_ID")
                ?: ""
            ).trim()
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
    }

    buildTypes {
        release {
            // O1: R8 code shrinking/obfuscation + resource shrinking. De-risked by the Gson→Moshi
            // migration (O5) — codegen adapters replace reflective field lookups — plus the
            // app-specific keep rules in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // O4: expose the exported Room schema JSONs (room.schemaLocation, below) to instrumented
    // tests so MigrationTestHelper can build a database at any historical version and assert a
    // Migration reaches the next one. Asset dir, not a resource, because the helper loads them
    // off the instrumentation context's assets at runtime.
    sourceSets {
        getByName("androidTest").assets.srcDirs(files("$projectDir/schemas"))
    }
}

// O4: Room writes a versioned schema JSON here on every build (one file per @Database version).
// These are committed to VCS — they are the source of truth a Migration is validated against.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.register("enforcePermissionBaseline") {
    description = "Enforces the zero-trust permission baseline by checking the AndroidManifest.xml"
    group = "verification"

    val manifestFile = file("src/main/AndroidManifest.xml")
    inputs.file(manifestFile)

    doLast {
        val allowedPermissions = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"
        )
        
        val manifestContent = manifestFile.readText()
        val permissionRegex = Regex("<uses-permission\\\\s+android:name=\\\"(.*?)\\\"")
        val foundPermissions = permissionRegex.findAll(manifestContent).map { it.groupValues[1] }.toSet()

        val illegalPermissions = foundPermissions - allowedPermissions
        if (illegalPermissions.isNotEmpty()) {
            throw GradleException(
                "Privacy Overreach Detected: The following permissions violate the Zero-Trust Baseline:\\n" +
                illegalPermissions.joinToString("\\n") +
                "\\nNo new permissions can be added without explicit architectural review."
            )
        }
    }
}

tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("enforcePermissionBaseline")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Persistent Immutable Data Structures for Compose UI Stability (HAMT)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.moshi)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.moshi)
    ksp(libs.squareup.moshi.kotlin.codegen)

    implementation(libs.google.dagger.hilt.android)
    ksp(libs.google.dagger.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.io.coil.compose)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.jsoup)
    implementation(libs.readability4j)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing) // O4: MigrationTestHelper

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
}
