/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 *
 * Sample per-module app for cmp-intent-launcher — targets Android + iOS + JVM Desktop + JS + wasmJs.
 * Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)

            implementation(project(":cmp-intent-launcher"))
            // The @Composable rememberIntentLauncher() lives in the -compose adapter
            // module. The core module is intentionally Compose-free (v0.3 split per
            // inter-app-comms-real-native-impls Phase 1) to reach all KMP targets.
            implementation(project(":cmp-intent-launcher-compose"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher"
            packageVersion = "1.0.0"
        }
    }
}
