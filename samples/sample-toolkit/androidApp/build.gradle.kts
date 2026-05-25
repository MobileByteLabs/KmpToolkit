/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.mobilebytelabs.kmptoolkit.samples.toolkit"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.mobilebytelabs.kmptoolkit.samples.toolkit"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(project(":samples:sample-toolkit:composeApp"))
    implementation(libs.androidx.activity.compose)
}
