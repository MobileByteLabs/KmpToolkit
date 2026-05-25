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
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-share
// ============================================================================
// Cross-platform share-sheet library. Payloads: text / url / image / file / multi.
// Targets: Android (Intent.ACTION_SEND), iOS (UIActivityViewController),
// macOS (NSSharingServicePicker), JVM Desktop (clipboard + FileDialog),
// JS / wasmJs (navigator.share + clipboard fallback).
// tvOS (v0.2): UIPasteboard clipboard-share fallback (no UIActivityViewController).
// watchOS (v0.2): UIActivityViewController N/A — onUnsupported fallback.
// Linux (v0.2): xdg-open URL share + xclip text/URL clipboard.
// mingw (v0.2): ShellExecuteW URL share + Win32 clipboard.
// (wasmWasi excluded — no DOM, no clipboard, no UI gesture surface.)
// iOS 14+ / macOS 11+ baseline per TS1.
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/
// v0.2 sub-plan: 10-platform-parity-v0-2.md
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.share"
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

    // tvOS (v0.2 — UIPasteboard fallback for text/url; image/file onUnsupported)
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    // watchOS (v0.2 — onUnsupported; no share-sheet surface)
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    // Linux + mingw (v0.2 — xdg-open / ShellExecuteW for URL share; clipboard fallback for text)
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
        name = "CMP Share"
        description =
            "Cross-platform share-sheet library for Kotlin Multiplatform — " +
            "text / url / image / file / multi payloads via Android Intent.ACTION_SEND, " +
            "iOS UIActivityViewController, macOS NSSharingServicePicker, JVM clipboard + FileDialog, " +
            "JS/wasmJs navigator.share with clipboard fallback."
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
