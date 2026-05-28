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
// v0.3 RESOLUTION (inter-app-comms-real-native-impls Phase 1): the Compose-Compiler
// module-level constraint is resolved by SPLITTING this module:
//   - cmp-intent-launcher (this file) — Compose-free core, 19 KMP targets
//   - cmp-intent-launcher-compose — holds @Composable rememberIntentLauncher(), 9 targets
// BREAKING: consumers of `rememberIntentLauncher()` must add `cmp-intent-launcher-compose`
// dep alongside this one. Migration: just add the dep — same import path, same API.
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-real-native-impls/
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

    // v0.3 expansion — Compose-free core reaches 19 KMP targets.
    // Stub actuals (UnsupportedPlatform) for the new ones; real impls land in
    // Phase 3 of inter-app-comms-real-native-impls.
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

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
            // androidx.activity provides ComponentActivity + registerForActivityResult (non-Compose path used by intentLauncher() extension)
            implementation("androidx.activity:activity:1.10.1")
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
