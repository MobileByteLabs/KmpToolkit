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

    // Dogfoods the published cmp-firebase Gradle plugin against a real consumer build.
    // Pinned to the last RELEASED version on Maven Central, deliberately: applying it from
    // the local build is impossible (samples are subprojects of this same build, and Gradle
    // cannot apply a plugin defined in the build applying it), and pinning the released
    // artifact is what an actual consumer resolves. Bump after each release.
    id("io.github.mobilebytelabs.firebase") version "3.5.23"
}

kotlin {
    android {
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
            // isStatic deliberately NOT set here — the cmp-firebase plugin forces it.
            // If the plugin ever stops doing that, this sample links a dynamic framework
            // and the assertion task below fails, which is the point of dogfooding it.
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
