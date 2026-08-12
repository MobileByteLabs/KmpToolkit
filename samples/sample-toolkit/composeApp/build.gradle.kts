/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 *
 * sample-toolkit/composeApp — unified catalog app showcasing every cmp-* library
 * in the KmpToolkit. Per-module samples remain in place; this is the single-app
 * showcase for new contributors / docs / store listings.
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.samples.toolkit.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()
    // js { browser() } and wasmJs { browser() } targets are intentionally NOT declared:
    // the catalog consumes :cmp-toast, :cmp-clipboard, :cmp-in-app-update and other
    // toolkit modules that do not yet expose JS / WasmJs targets. The root
    // :kotlinNpmInstall task fails to resolve those project dependencies for the JS
    // variant, blocking jsNodeTest / wasmJsNodeTest for sibling modules. Restore
    // these targets only after every :cmp-* module declared in commonMain.dependencies
    // below exposes a matching JS / WasmJs target. Tracked: PLAN
    // cmp-network-monitor-hardening / Phase 04 T0.

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
            implementation(libs.navigation.compose)

            // Toolkit libraries available on every catalog platform (Android + iOS + Desktop)
            implementation(project(":cmp-clipboard"))
            implementation(project(":cmp-toast"))
            implementation(project(":cmp-in-app-update"))
            implementation(project(":cmp-bubble"))
            implementation(project(":cmp-open-url"))
            implementation(project(":cmp-deep-link"))
            implementation(project(":cmp-network-monitor"))
            implementation(project(":cmp-network-monitor-compose"))
            implementation(project(":cmp-firebase"))
            implementation(project(":cmp-pdf-generator"))
            implementation(project(":cmp-share"))
            implementation(project(":cmp-intent-launcher"))
            implementation(project(":cmp-intent-launcher-compose"))
            implementation(project(":cmp-app-intents"))
            implementation(project(":cmp-product-tickets"))
            implementation(project(":cmp-remote-config"))
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
        mainClass = "com.mobilebytelabs.kmptoolkit.samples.toolkit.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sample-toolkit"
            packageVersion = "1.0.0"
        }
    }
}
