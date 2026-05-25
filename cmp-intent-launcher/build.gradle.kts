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
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-intent-launcher
// ============================================================================
// Typed Android-Intent builder DSL with ActivityResult contracts.
// Android: full Intent surface + rememberLauncherForActivityResult bridge.
// iOS: whitelist mappings (PHPickerViewController, UIDocumentPickerViewController,
// CNContactPickerViewController) — common picker contracts; arbitrary actions
// route to onUnsupported lambda or Failed(NoHandler).
// JVM Desktop: AWT FileDialog for picker contracts; other actions unsupported.
// JS / wasmJs: <input type=file> for picker; user-gesture constraint per TS6.
// Per Phase 0 TS3: primary API is @Composable rememberIntentLauncher();
// Android escape hatch ComponentActivity.intentLauncher() extension.
// (Top-level suspend `intent { }` DROPPED per TS3 — unworkable across platforms.)
//
// v0.2 (sub-plan 10) constraint discovery: the Compose Compiler Gradle plugin
// (alias(libs.plugins.composeCompiler)) is MODULE-LEVEL, not source-set-level —
// it requires compose.runtime on the classpath for EVERY target. Adding
// tvOS/watchOS/Linux/mingw targets fails with "The Compose Compiler requires the
// Compose Runtime to be on the class path". The composeMain intermediate
// source-set workaround does NOT solve this because the compiler plugin runs
// before source-set resolution.
//
// Honest resolution: cmp-intent-launcher stays at 9 targets in v0.2.
// To reach 19-target parity in a future v0.3, options are:
//   (a) Split into cmp-intent-launcher-core (non-Compose, all 19 targets) +
//       cmp-intent-launcher (current API, depends on -core, 9 targets)
//   (b) Drop @Composable rememberIntentLauncher() — breaking API change
// Documented in sub-plan 10 + module README.
// (tvOS / watchOS / Linux / mingw / wasmWasi excluded per Compose-MP constraint.)
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.intentlauncher"
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
            implementation(compose.runtime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.androidx.activity.compose)
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
        name = "CMP Intent Launcher"
        description =
            "Typed Intent builder DSL with ActivityResult contracts for Kotlin Multiplatform — " +
            "Android full Intent surface with rememberLauncherForActivityResult bridge, " +
            "iOS PHPicker/DocumentPicker/ContactPicker whitelist, JVM Desktop AWT FileDialog, " +
            "JS/wasmJs <input type=file>. Composable-scoped API + Android Activity-extension escape hatch."
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
