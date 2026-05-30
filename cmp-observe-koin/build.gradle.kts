/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-observe-koin
// ============================================================================
// Zero-config Koin companion for cmp-observe.
//
// Consumer apps that use Koin call observeKoinModule(...) at startup; the module
// reads consent state + observability_opt_in flags and registers only the hooks
// the consumer + end-user have agreed to.
//
// Per GOAL.md D8 progressive consent posture:
// - T0 + T1 default ON
// - T2 + T3 + T4 opt-in
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.observe.koin"
        compileSdk = 36
        minSdk = 24
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach { it.binaries.framework { baseName = "CmpObserveKoin" } }

    js(IR) { browser(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonMain.dependencies {
            api(project(":cmp-observe"))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "cmp-observe-koin", version.toString())
    pom {
        name.set("cmp-observe-koin")
        description.set("Koin companion for cmp-observe — zero-config DI registration of LibraryObservationHook instances.")
        inceptionYear.set("2026")
        url.set("https://github.com/MobileByteLabs/KmpToolkit")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
    }
}
