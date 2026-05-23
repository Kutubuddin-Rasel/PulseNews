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
        
        val geminiApiKey = (
            providers.gradleProperty("GEMINI_API_KEY").orNull
                ?: localProperties.getProperty("GEMINI_API_KEY")
                ?: System.getenv("GEMINI_API_KEY")
                ?: ""
            ).trim()
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.squareup.okhttp)

    implementation(libs.google.dagger.hilt.android)
    ksp(libs.google.dagger.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
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
    implementation(libs.google.ai.client.generativeai)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
