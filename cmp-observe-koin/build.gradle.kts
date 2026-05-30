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
// observeKoinModule(hooks: List<LibraryObservationHook>) takes hook instances
// as a parameter (no direct dep on Firebase hook types) so the module ships
// to all 10 KMP targets — same set as cmp-observe (audit follow-up 2026-05-30).
// The 6 stub targets (js/wasmJs/tvos/watchos/linux/mingw) get the module but
// typically pass an empty hook list or consumer-provided non-Firebase hooks
// (GitLive Firebase Crashlytics/Performance have no js/wasmJs variants).
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

    // ─── Stub-target additions (2026-05-30 — matches cmp-observe's 10-target set) ──
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosX64()
    watchosArm64()
    watchosSimulatorArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

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
