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
    alias(libs.plugins.kotlinxSerialization)
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-app-intents
// ============================================================================
// Declarative App Intents DSL for SiriKit Shortcuts + Spotlight (iOS 16+).
// Kotlin DSL emits a JSON manifest at runtime.
// iOS: ship CmpAppIntentBridge.swift + per-intent stub template (per Phase 0 TS2 —
// consumer copies the .swift file into their Xcode target; NO Package.swift / SPM).
// Android: on-device runtime registry + BroadcastReceiver (v0.1 scope — Google
// Assistant integration deferred to v0.2 cmp-app-intents-assistant per TS7).
// JVM Desktop + JS / wasmJs: no-op + invokeForTesting helper.
// watchOS: full impl via existing manifest + Swift bridge (watchOS 10+ Shortcuts).
// tvOS: Siri Suggestions via manifest; CoreSpotlight indexing skipped (iOS/macOS only).
// Linux / mingw: registry-only + manifest JSON to XDG / APPDATA dir.
// (wasmWasi excluded — no UI surface available.)
// iOS 16+ enforced via runtime if #available checks in shipped Swift code (TS1).
// v0.2 sub-plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/10-platform-parity-v0-2.md
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.appintents"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        androidResources.enable = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    // tvOS targets (v0.2 — manifest only; Spotlight indexing skipped, no CoreSpotlight on tvOS)
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    // watchOS targets (v0.2 — FULL App Intents impl; watchOS 10+ Shortcuts via Swift bridge)
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    // Linux + mingw (v0.2 — registry-only; manifest JSON to XDG / APPDATA dir)
    linuxX64()
    linuxArm64()
    mingwX64()

    js {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs()
    }

    wasmJs {
        browser()
        nodejs()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test.junit)
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING
// ============================================================================
mavenPublishing {
    signAllPublications()

    pom {
        name = "CMP App Intents"
        description =
            "Declarative App Intents DSL for Kotlin Multiplatform — " +
            "SiriKit Shortcuts + Spotlight (iOS 16+) via ship-source-file Swift bridge, " +
            "Android on-device runtime registry (Assistant integration deferred to v0.2), " +
            "JVM Desktop + JS/wasmJs no-op fallback. Per-intent declarative DSL with parameter types."
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
