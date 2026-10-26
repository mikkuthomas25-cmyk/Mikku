plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Login needs a Firebase project. Without app/google-services.json the app still
// builds and offers "continue without account" so it can be tried immediately.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.fintrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fintrack.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.$versionCode"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
    }

    signingConfigs {
        // A fixed key so every CI build installs over the previous one and the
        // SHA-1 registered in Firebase (needed for Google sign-in) stays the same.
        getByName("debug") {
            storeFile = file("fintrack-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.getByName("debug") }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/INDEX.LIST",
                "META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/*.md", "META-INF/versions/9/module-info.class",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            )
        }
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Login: Firebase Auth (email/password) + Google via Credential Manager.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
