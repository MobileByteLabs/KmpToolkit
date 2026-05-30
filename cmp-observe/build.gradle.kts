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
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.binaryCompatibilityValidator)
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-observe
// ============================================================================
// Shared library observability hook interface + 4 default hook implementations.
// Authored 2026-05-30 by library-runtime-observability epic Phase 01.
//
// Every published cmp-* / worker-kmp / monetization-kmp / paycraft library MAY
// add this as a commonMain dependency to receive the LibraryObservationHook
// interface — then call LibraryObservation.notifyInit(...) at init paths.
//
// Hook implementations:
// - FirebaseCrashlyticsAttributionHook  → T0 (setCustomValue per library version)
// - FirebaseAnalyticsHealthHook         → T1 (lib_init_success / lib_init_failure events)
// - FirebasePerformanceHook             → T3 (Trace.start/stop around *_start/*_end lifecycle events)
// - SupabaseEventsHook                  → T2/T4 (structured events to framework-supabase.library_events)
//
// All hooks are FAIL-SAFE: exceptions swallowed by LibraryObservation.safeCall;
// no hook can crash the host application.
//
// Targets: full KMP (Android, iOS x64/arm64/simArm64, macOS x64/arm64, JVM, JS, wasmJs)
// excluded native/Tier-3: tvOS, watchOS, linux, mingw, wasmWasi (no Firebase + Supabase wiring on those targets).
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.observe"
        compileSdk = 36
        minSdk = 24
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach { it.binaries.framework { baseName = "CmpObserve" } }

    js(IR) {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.gitlive.firebase.crashlytics)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.performance)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "cmp-observe", version.toString())
    pom {
        name.set("cmp-observe")
        description.set("Shared library observability hook interface + 4 default Firebase/Supabase hook implementations. Per RULE-LIB-OBSERVABILITY-SURFACE-001.")
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
