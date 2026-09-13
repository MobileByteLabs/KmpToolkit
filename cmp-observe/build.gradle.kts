/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
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
//   via cmp-observe-firebase (a separate artifact since 2026-09-13). The GitLive Firebase
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

    android {
        namespace = "com.mobilebytelabs.kmptoolkit.observe"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        // JVM 11, matching every consumer. This module is the universal dependency and `observeInit`
        // is an INLINE function, so its bytecode is inlined into the caller: built higher than the
        // consumer, every Android compilation fails with
        // "Cannot inline bytecode built with JVM target 17 into bytecode being built with JVM target 11".
        // Caught on cmp-deep-link the first time an init provider used the helper.
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    // No `binaries.framework { baseName = "CmpObserve" }` block — that triggers
    // the iOS Framework link step which fails on `ld: framework 'FirebaseCore'
    // not found`. GitLive Firebase 3.x links the native Apple SDK via SwiftPM,
    // and that resolution only happens in the CONSUMING app's build (the
    // `embedAndSignAppleFrameworkForXcode` phase resolves `firebase-ios-sdk` and
    // generates the synthetic Swift package). A library module linking its own
    // Framework has no such step, so FirebaseCore is not on the linker path.
    // Consumer apps build the Framework on their side. Following the cmp-firebase
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
    linuxX64()
    linuxArm64()
    mingwX64()

    // watchOS + wasmWasi added 2026-09-13 so cmp-observe can be a commonMain dependency of the
    // 21-target modules. Until now it shipped 15, and a module depending on it in commonMain would
    // have been capped at that — which is why cmp-network-monitor's dependency is androidMain-only
    // and why 22 of 23 modules generate CmpMetadata but never report anything.
    //
    // Nothing here needs a dependency: the core (LibraryObservation / LibraryObservationHook /
    // CmpMetadata) is pure stdlib — kotlin.concurrent.atomics for the registry — and the GitLive
    // Firebase hooks stay in `firebaseHooksMain`, which is attached only to androidMain + iosMain.
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            // NO DEPENDENCIES, deliberately. This module is the universal one: every cmp-* module
            // generates CmpMetadata and depends on this to report itself, so anything added here lands
            // on every consumer's classpath.
            //
            // The three GitLive Firebase hooks lived here until 2026-09-13 and are now
            // cmp-observe-firebase. They pulled the Firebase BOM and three GitLive artifacts into the
            // android and ios variants: adding `implementation(project(":cmp-observe"))` to cmp-share
            // took its Android runtime classpath from 73 lines / 0 Firebase entries to 558 / 91. An
            // app copying a string to the clipboard does not need an analytics SDK.
            //
            // What remains is pure stdlib — the registry (kotlin.concurrent.atomics), the hook
            // interface, CmpMetadata, and the observe* helpers — which is why this ships all 21
            // targets with no intermediate source sets.
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    // Bundle Dokka v2 HTML output inside -javadoc.jar so consumers browsing
    // Maven Central artifacts get real API docs rather than an empty jar.
    // Task name is the Dokka v2 ID; the DokkaConventionPlugin in build-logic
    // registers it via `org.jetbrains.dokka` + DokkaExtension.
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true,
        ),
    )
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
