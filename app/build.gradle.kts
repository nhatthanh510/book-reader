import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 ships built-in Kotlin support, so no kotlin-android plugin here. AGP detects
    // this plugin and wires the Compose compiler into the built-in Kotlin compilation.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// The Google Books API key is read from local.properties (git-ignored) so it never lands in
// version control. Missing key is not a build failure: the app starts and shows an error state.
val googleBooksApiKey: String = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
}.getProperty("GOOGLE_BOOKS_API_KEY").orEmpty()

if (googleBooksApiKey.isBlank()) {
    logger.warn(
        "\nGOOGLE_BOOKS_API_KEY is not set, so the app will start with no book data." +
            "\nCopy local.properties.example to local.properties and add your key." +
            "\nSee README.md for how to create one.\n"
    )
}

android {
    namespace = "com.example.docsach"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.docsach"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.coil.compose)
}
