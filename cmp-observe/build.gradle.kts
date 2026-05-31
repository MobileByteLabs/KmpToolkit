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
// LIBRARY CONFIGURATION — cmp-observe
// ============================================================================
// Shared library observability hook interface + 3 default Google/Firebase hook
// implementations. Authored 2026-05-30 by library-runtime-observability epic
// Phase 01; Supabase backend support dropped 2026-05-31 (Google-only scope).
//
// Every published cmp-* / worker-kmp / monetization-kmp / paycraft library MAY
// add this as a commonMain dependency to receive the LibraryObservationHook
// interface — then call LibraryObservation.notifyInit(...) at init paths.
//
// Hook implementations (Google/Firebase only):
// - FirebaseCrashlyticsAttributionHook  → T0 (setCustomKey per library version)
// - FirebaseAnalyticsHealthHook         → T1 (lib_init_success / lib_init_failure events)
// - FirebasePerformanceHook             → T3 (Trace.start/stop around *_start/*_end lifecycle events)
//
// All hooks are FAIL-SAFE: exceptions swallowed by LibraryObservation.safeCall;
// no hook can crash the host application.
//
// Targets: 10/10 KMP coverage.
// - 2 "Firebase-supported" targets (android + ios) get the 3 Firebase hook impls
//   via the firebaseHooksMain intermediate source-set. The 3 GitLive Firebase
//   deps at v2.4.0 only intersect on {android, ios} — crashlytics has no jvm
//   variant, perf has no macos variant, none have js/wasmJs/native.
// - 8 "stub" targets (jvm, macos, js, wasmJs, tvos, watchos, linux, mingw) inherit
//   ONLY commonMain — they get the LibraryObservationHook interface +
//   LibraryObservation registry + CmpMetadata data class (no transport, no hooks).
//   Consumer apps register their own no-op or platform-specific hooks for these
//   targets if they need crash/analytics attribution.
//
// This structure eliminates the commonMain-scope blast-radius problem surfaced
// by cmp-network-monitor (11 targets) depending on cmp-observe: every cmp-*
// module can depend on cmp-observe in commonMain without constraining its own
// target list.
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.observe"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    // No `binaries.framework { baseName = "CmpObserve" }` block — that triggers
    // the iOS Framework link step which fails on `ld: framework 'FirebaseCore'
    // not found` because GitLive Firebase relies on CocoaPods-provisioned Firebase
    // Apple frameworks at link time. Consumer apps add the Firebase CocoaPods +
    // build the Framework on their side. Following the cmp-firebase-analytics
    // pattern (klib-only publication for iOS).

    js(IR) {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

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
            // Interface + registry + CmpMetadata only — no transport, no deps.
            // The 3 Google/Firebase hook impls live in firebaseHooksMain (below)
            // and bring their own GitLive Firebase deps. GitLive Firebase doesn't
            // publish for js/wasmJs/tvos/watchos/linux/mingw — keeping it out of
            // commonMain lets cmp-observe ship 10 targets.
        }

        // Custom intermediate source-set: holds the 3 Firebase hook impls + their deps.
        // Only android + ios depend on this source-set (the platform intersection of all
        // 3 GitLive Firebase deps); the 8 stub targets (jvm/macos/js/wasmJs/tvos/watchos/
        // linux/mingw) skip it and get an interface-only commonMain compilation.
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

        // Wire ONLY android + ios to depend on firebaseHooksMain — the platform
        // intersection of all 3 GitLive Firebase deps at v2.4.0:
        //   - firebase-crashlytics : android + ios + macos       (NO jvm, NO js)
        //   - firebase-perf        : android + ios + jvm         (NO macos, NO js)
        //   - firebase-analytics   : android + ios + jvm + macos (NO js)
        // Intersection = { android, ios }. Bundling all 3 in firebaseHooksMain
        // means jvm fails on crashlytics, macos fails on perf, js fails on all 3.
        // KGP 2.x strictly validates per-target dep availability across shared
        // source-sets and rejects the build on any unresolved platform.
        // Net: 8 stub targets (jvm, macos, js, wasmJs, tvos, watchos, linux, mingw)
        // inherit ONLY commonMain — they get the LibraryObservationHook interface +
        // LibraryObservation registry + CmpMetadata data class only (no hook impls).
        // Apps targeting those platforms register consumer-provided hooks if they
        // need crash/analytics attribution.
        androidMain.get().dependsOn(firebaseHooksMain)
        iosMain.get().dependsOn(firebaseHooksMain)

        // Firebase BOM supplies versions for com.google.firebase:* on Android.
        // GitLive's firebase-{crashlytics,analytics,perf}-android transitively
        // depend on com.google.firebase:firebase-{crashlytics,analytics,perf}
        // WITHOUT pinned versions — the BOM resolves them. Without this, Gradle
        // fails with "Could not find com.google.firebase:firebase-crashlytics:."
        // (empty version). Matches the cmp-firebase-analytics pattern.
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    signAllPublications()

    pom {
        name = "CMP Observe"
        description =
            "Shared library observability hook interface + 3 default Google/Firebase hook " +
            "implementations (Crashlytics attribution / Analytics health / Performance traces) " +
            "for Kotlin Multiplatform. Per RULE-LIB-OBSERVABILITY-SURFACE-001."
        inceptionYear = "2026"
        url = "https://github.com/MobileByteLabs/KmpToolkit/"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "MobileByteLabs"
                name = "MobileByteLabs"
                url = "https://github.com/MobileByteLabs"
            }
        }

        scm {
            url = "https://github.com/MobileByteLabs/KmpToolkit/"
            connection = "scm:git:git://github.com/MobileByteLabs/KmpToolkit.git"
            developerConnection = "scm:git:ssh://git@github.com/MobileByteLabs/KmpToolkit.git"
        }
    }
}
