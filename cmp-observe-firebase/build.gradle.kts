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

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-observe-firebase
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
//   via cmp-observe-firebase-firebase (a separate artifact since 2026-09-13). The GitLive Firebase
//   deps at v2.4.0 only intersect on {android, ios} — crashlytics has no jvm
//   variant, perf has no macos variant, none have js/wasmJs/native.
// - 8 "stub" targets (jvm, macos, js, wasmJs, tvos, watchos, linux, mingw) inherit
//   ONLY commonMain — they get the LibraryObservationHook interface +
//   LibraryObservation registry + CmpMetadata data class (no transport, no hooks).
//   Consumer apps register their own no-op or platform-specific hooks for these
//   targets if they need crash/analytics attribution.
//
// This structure eliminates the commonMain-scope blast-radius problem surfaced
// by cmp-network-monitor (11 targets) depending on cmp-observe-firebase: every cmp-*
// module can depend on cmp-observe-firebase in commonMain without constraining its own
// target list.
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    // FOUR targets only — the platform intersection of the three GitLive Firebase artifacts, which is
    // exactly the set the old `firebaseHooksMain` source set was attached to. GitLive does not publish
    // for jvm/macos/js/wasmJs/tvos/watchos/linux/mingw/wasmWasi, which is why these hooks could never
    // have lived in a 21-target module's commonMain.
    android {
        namespace = "com.mobilebytelabs.kmptoolkit.observe.firebase"
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

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: a consumer registering these hooks holds
            // LibraryObservationHook and CmpMetadata in their own code.
            api(project(":cmp-observe"))

            implementation(libs.gitlive.firebase.crashlytics)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.performance)
        }

        // Firebase BOM supplies versions for com.google.firebase:* on Android. GitLive's
        // firebase-{crashlytics,analytics,perf}-android depend on com.google.firebase:* WITHOUT pinned
        // versions — the BOM resolves them. Without it Gradle fails with
        // "Could not find com.google.firebase:firebase-crashlytics:." (empty version).
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
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
        name = "CMP Observe Firebase"
        description =
            "GitLive Firebase hook implementations for cmp-observe — Crashlytics attribution, " +
            "Analytics health, Performance traces. Add alongside cmp-observe to forward library " +
            "lifecycle events to Firebase."
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
