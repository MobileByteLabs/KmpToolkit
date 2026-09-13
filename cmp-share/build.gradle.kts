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
    // v0.4 Phase 9 — ABI stability (kover deferred — incompatible with the
    // new `com.android.kotlin.multiplatform.library` plugin's `androidLibrary {}`
    // extension; Kover 0.9.1 still requires the legacy `android {}` extension.
    // Re-enable when Kover ships KMP-Android-Library plugin support.)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-share
// ============================================================================
// Cross-platform share-sheet library. Payloads: text / url / image / file / multi.
// Targets: Android (Intent.ACTION_SEND), iOS (UIActivityViewController),
// macOS (NSSharingServicePicker), JVM Desktop (clipboard + FileDialog),
// JS / wasmJs (navigator.share + clipboard fallback).
// tvOS: UIPasteboard clipboard-share fallback (no UIActivityViewController).
// watchOS: WCSession.transferUserInfo handoff to the paired iPhone (text/url; binary N/A).
// Linux (v0.2): xdg-open URL share + xclip text/URL clipboard.
// mingw (v0.2): ShellExecuteW URL share + Win32 clipboard.
// wasmWasi: declared UnsupportedPlatform for every payload — no DOM, no clipboard, no gesture
// surface. Present so a WASI consumer resolves the artifact and compiles shared code.
// 21 targets total — the full KMP matrix.
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

    android {
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

        // Without this there is no androidHostTest variant at all, so the commonTest suite —
        // including everything asserting Android's own `platformShareCapabilities` — never ran
        // on the Android target. Enabling it is what makes `testAndroidHostTest` exist.
        withHostTestBuilder {}.configure {
            // android.jar in a JVM host test is a stub whose methods THROW by default, so a
            // framework call aborts a test even when the code under test handled the situation
            // correctly. Returning defaults lets the real behaviour be asserted instead.
            isReturnDefaultValues = true
        }
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

    // watchOS — Text/Url handed to the paired iPhone via WCSession.transferUserInfo, which the
    // companion app presents in a real UIActivityViewController. All five architectures build:
    // the handoff passes only Kotlin primitives, so the NSUInteger/size_t bit-width mismatch that
    // blocks the BINARY path on the 32-bit watchosArm32 never arises here.
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    // Linux + mingw (v0.2 — xdg-open / ShellExecuteW for URL share; clipboard fallback for text)
    linuxX64()
    linuxArm64()
    mingwX64 {
        // SPIKE Phase 0 S0.A — revert if verdict FAIL
        compilations.getByName("main").cinterops {
            create("win32clipboard") {
                defFile = file("src/mingwMain/cinterop/win32-clipboard.def")
            }
        }
    }

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

    // wasmWasi — no DOM, no clipboard, no gesture surface, so every payload reports a DECLARED
    // UnsupportedPlatform. It is here so a WASI consumer can depend on cmp-share and compile its
    // shared code, rather than the dependency failing to resolve.
    wasmWasi {
        nodejs()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        // koin-core publishes every target this module builds EXCEPT wasmWasi. Rather than drop
        // wasmWasi or split off a `cmp-share-koin` artifact, the Koin binding lives in an
        // intermediate source set covering the other 20 targets — so `shareModule` ships
        // everywhere it can, and the wasmWasi variant simply has no Koin on its classpath.
        //
        // Only `di/ShareModule.kt` is in here. ShareManager, ShareManagerImpl, ShareCapabilities
        // and the Share engine all stay in commonMain and are Koin-free, so a consumer on Hilt,
        // Kodein or hand-rolled wiring binds ShareManagerImpl itself and never loads a Koin class.
        val koinMain = create("koinMain").apply { dependsOn(getByName("commonMain")) }
        val koinTest = create("koinTest").apply { dependsOn(getByName("commonTest")) }
        listOf("jvmMain", "androidMain", "appleMain", "linuxMain", "mingwMain", "jsMain", "wasmJsMain")
            .forEach { getByName(it).dependsOn(koinMain) }
        listOf("jvmTest", "androidHostTest", "appleTest", "linuxTest", "mingwTest", "jsTest", "wasmJsTest")
            .forEach { getByName(it).dependsOn(koinTest) }

        koinMain.dependencies {
            // `implementation`, not `api` — not forced onto the consumer's compile classpath.
            implementation(libs.koin.core)
        }

        commonMain.dependencies {

            // Reports this library's lifecycle to hooks the consumer registered. Pure stdlib since
            // the Firebase hooks moved to cmp-observe-firebase — 2 classpath lines, no transitive SDK.
            implementation(project(":cmp-observe"))
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

// Library Runtime Observability — auto-generate CmpMetadata.kt for cmp-observe hooks (epic 2026-05-30)
apply(from = "$rootDir/cmp-observe-metadata.gradle.kts")
