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
// Targets: 10/10 KMP coverage as of 2026-05-30 audit follow-up + PR #115 CI fix.
// - 4 "Firebase-supported" targets (jvm/android/ios/macos) get the 3 Firebase hook impls
//   (Crashlytics/Analytics/Performance) via firebaseHooksMain intermediate source-set.
//   GitLive Firebase Crashlytics/Performance do not publish js/wasmJs variants; including
//   js+wasmJs in firebaseHooksMain consumers breaks dependency resolution.
// - 6 "stub" targets (js, wasmJs, tvos, watchos, linux, mingw) inherit ONLY commonMain —
//   they get the LibraryObservationHook interface + LibraryObservation registry +
//   CmpMetadata data class + SupabaseEventsHook (Ktor-backed, all-target). Firebase hooks
//   are NOT available on these targets — consumer apps register their own no-op or
//   platform-specific hooks for crash/analytics attribution if those backends ever ship.
//
// This structure was added to eliminate the commonMain-scope blast-radius problem
// surfaced by cmp-network-monitor (11 targets) depending on cmp-observe (originally
// 7 targets): every cmp-* module can now depend on cmp-observe in commonMain without
// constraining its own target list.
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

    // ─── Stub-target additions (2026-05-30 audit follow-up) ──────────────
    // These 4 target groups only see commonMain — NO Firebase hook impls.
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // Ktor supports all 10 KMP targets via per-platform engines — SupabaseEventsHook
            // can stay in commonMain and emit T2/T4 events from any target.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Firebase deps INTENTIONALLY MOVED to firebaseHooksMain (below) — GitLive Firebase
            // doesn't publish for js/wasmJs/tvos/watchos/linux/mingw. Keeping them out of
            // commonMain is what lets cmp-observe ship 10 targets.
        }

        // Custom intermediate source-set: holds the 3 Firebase hook impls + their deps.
        // Only the 4 Firebase-supported targets (jvm/android/ios/macos) depend on this
        // source-set; the 6 stub targets (js/wasmJs/tvos/watchos/linux/mingw) skip it and
        // get an interface-only commonMain compilation.
        //
        // Hook files physically live in src/firebaseHooksMain/kotlin/.../hooks/.
        val firebaseHooksMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.gitlive.firebase.crashlytics)
                implementation(libs.gitlive.firebase.analytics)
                implementation(libs.gitlive.firebase.performance)
            }
        }

        // Wire the 4 Firebase-supported targets to depend on firebaseHooksMain.
        // GitLive Firebase Crashlytics/Analytics/Performance publish variants for:
        // jvm, android, ios (X64/Arm64/SimulatorArm64), macos (X64/Arm64) — that's it.
        // js + wasmJs are deliberately OMITTED because Crashlytics has no js variant
        // (resolution fails with "No matching variant of dev.gitlive:firebase-crashlytics
        // was found ... attribute org.jetbrains.kotlin.platform.type with value 'js'").
        // tvos/watchos/linux/mingw also omitted — they get commonMain only.
        // Net: 6 targets see ONLY the interface + Ktor-based SupabaseEventsHook
        // (js, wasmJs, tvos, watchos, linux, mingw). Apps targeting those platforms
        // register consumer-provided hooks if they need crash/analytics attribution.
        jvmMain.get().dependsOn(firebaseHooksMain)
        androidMain.get().dependsOn(firebaseHooksMain)
        iosMain.get().dependsOn(firebaseHooksMain)
        macosMain.get().dependsOn(firebaseHooksMain)

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
