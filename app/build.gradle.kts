/*
 * app/build.gradle.kts
 * Only the lines marked ──► are new or changed
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // ──► JSON for our script files
    alias(libs.plugins.kotlin.serialization)          // add this to libs.versions.toml
}

android {
    namespace = "com.cfa.immortalautomation"
    compileSdk = 34            // ──► stick to API 34 (stable Android 14)

    defaultConfig {
        applicationId = "com.cfa.immortalautomation"
        minSdk = 26            // ──► dispatchGesture() needs at least 24, no reason to be 34
        targetSdk = 34         // ──► matches compileSdk
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17   // ──► AGP 8 lets you go 17; optional
        targetCompatibility = JavaVersion.VERSION_17   // ──►
    }
    kotlinOptions { jvmTarget = "17" }                 // ──► keep toolchain consistent

    buildFeatures {
        compose = true
        viewBinding = true      // ──► needed for overlay XML
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"       // ──► or whatever Compose BOM uses
    }
}

dependencies {
    // --- AndroidX core / Compose you already had ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- NEW: coroutines & JSON (script storage) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    // --- tests unchanged ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
