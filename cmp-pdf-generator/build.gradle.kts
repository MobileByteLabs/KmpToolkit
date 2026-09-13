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
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
    alias(libs.plugins.binaryCompatibilityValidator)
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-pdf-generator
// ============================================================================
// Cross-platform PDF generation library. HTML / Markdown / DSL input modes;
// File / ByteArray / URI / Share / Print / Save output destinations.
// Targets: Android, iOS (14+), macOS (11+), JVM, JS, wasmJs.
// (tvOS / watchOS / Linux / mingw / wasmWasi excluded — upstream library
// coverage incomplete; kotlinx-html doesn't publish for those targets.)
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-pdf-generator/
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    android {
        namespace = "io.github.mobilebytelabs.kmptoolkit.pdfgenerator"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        withJava()

        withHostTestBuilder {}.configure {}

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources.enable = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    // tvOS / watchOS / Linux / Windows / WASI — no HTML renderer exists on any of them, which is
    // why this module shipped on 9 targets while the rest of the toolkit reached 21. They share a
    // single `fallbackMain` actual built on TextPdfWriter: PDF is a file format, so emitting one
    // needs no OS service, only the absence of HTML layout.
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

    wasmWasi {
        nodejs()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        // One actual for every target with no HTML renderer. tvOS and watchOS also sit under
        // appleMain (from the default hierarchy), which is fine — a source set may depend on more
        // than one parent.
        val fallbackMain = create("fallbackMain").apply { dependsOn(getByName("commonMain")) }
        listOf(
            "tvosMain",
            "watchosMain",
            "linuxMain",
            "mingwMain",
            "wasmWasiMain",
        ).forEach { getByName(it).dependsOn(fallbackMain) }

        // NSData <-> ByteArray helpers, for the two Apple targets whose renderers need them.
        // They CANNOT live in appleMain: `NSData.length` is NSUInteger, which is 32-bit on
        // watchOS (arm64_32 is an ILP32 ABI) and 64-bit on iOS/macOS, and Kotlin/Native rejects a
        // source set spanning both widths. Scoping them here is what lets watchOS join the matrix
        // at all — it reaches PDF generation through fallbackMain, which touches no NSData.
        val darwinDocMain = create("darwinDocMain").apply { dependsOn(getByName("commonMain")) }
        listOf("iosMain", "macosMain").forEach { getByName(it).dependsOn(darwinDocMain) }

        // Test twin of darwinDocMain — the WKWebView-backed Apple targets, and ONLY those.
        //
        // A WebView-route test CANNOT live in `appleTest`: the default hierarchy feeds appleTest to
        // watchOS and tvOS as well, and those targets have no WKWebView — they resolve PdfGenerator
        // from `fallbackMain`. A test asserting `PdfError.RenderTimeout` therefore passes on
        // iOS/macOS and fails on watchOS/tvOS, which is exactly what happened: the watchOS CI job
        // went red on `PdfRenderTimeoutTest` while the same test was green on both targets a macOS
        // dev box can actually run (a local box has no watchOS simulator runtime — see
        // native-tests.yml).
        val darwinDocTest = create("darwinDocTest").apply { dependsOn(getByName("commonTest")) }
        listOf("iosTest", "macosTest").forEach { getByName(it).dependsOn(darwinDocTest) }

        // `kotlinx-html` does not publish for wasmWasi, and the HTML compiler / templates are the
        // only things that use it. Moving them here keeps wasmWasi in the matrix with the parts
        // that ARE portable — the PdfDocument DSL and TextPdfWriter — which is the combination a
        // server-side WASI report generator actually needs.
        val htmlMain = create("htmlMain").apply { dependsOn(getByName("commonMain")) }
        val htmlTest = create("htmlTest").apply { dependsOn(getByName("commonTest")) }
        listOf(
            "androidMain",
            "appleMain",
            "jsMain",
            "jvmMain",
            "linuxMain",
            "mingwMain",
            "wasmJsMain",
        ).forEach { getByName(it).dependsOn(htmlMain) }
        listOf(
            "iosTest",
            "jsTest",
            "jvmTest",
            "linuxTest",
            "macosTest",
            "mingwTest",
            "wasmJsTest",
        ).forEach { getByName(it).dependsOn(htmlTest) }

        htmlMain.dependencies {
            implementation(libs.kotlinx.html)
        }

        // `org.jetbrains:markdown` does not publish for tvOS device/x64, watchOS or wasmWasi, so
        // the Markdown adapter lives in a source set covering only the targets it can reach.
        // Moving the dependency rather than dropping the targets keeps Markdown->PDF available
        // everywhere it was before and adds plain PDF generation everywhere else.
        // Depends on htmlMain, not commonMain: the Markdown adapter renders through
        // HtmlTemplateGenerator, and markdown's target set is a subset of kotlinx-html's.
        val markdownMain = create("markdownMain").apply { dependsOn(htmlMain) }
        val markdownTest = create("markdownTest").apply { dependsOn(htmlTest) }
        listOf(
            "androidMain",
            "iosMain",
            "jsMain",
            "jvmMain",
            "linuxMain",
            "macosMain",
            "mingwMain",
            "wasmJsMain",
        ).forEach { getByName(it).dependsOn(markdownMain) }
        listOf(
            "iosTest",
            "jsTest",
            "jvmTest",
            "linuxTest",
            "macosTest",
            "mingwTest",
            "wasmJsTest",
        ).forEach { getByName(it).dependsOn(markdownTest) }

        markdownMain.dependencies {
            implementation(libs.markdown)
        }

        // koin-core publishes no wasmWasi artifact, so the Koin binding lives in an
        // intermediate source set spanning the other 20 targets. Same as every other module.
        val koinMain = create("koinMain").apply { dependsOn(getByName("commonMain")) }
        val koinTest = create("koinTest").apply { dependsOn(getByName("commonTest")) }
        listOf("jvmMain", "androidMain", "appleMain", "linuxMain", "mingwMain", "jsMain", "wasmJsMain")
            .forEach { getByName(it).dependsOn(koinMain) }
        listOf("jvmTest", "appleTest", "linuxTest", "mingwTest", "jsTest", "wasmJsTest")
            .forEach { getByName(it).dependsOn(koinTest) }

        koinMain.dependencies {
            implementation(libs.koin.core)
        }

        commonMain.dependencies {

            // Reports this library's lifecycle to hooks the consumer registered. Pure stdlib since
            // the Firebase hooks moved to cmp-observe-firebase — 2 classpath lines, no transitive SDK.
            implementation(project(":cmp-observe"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.kotlinx.coroutines.android)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmMain.dependencies {
            implementation(libs.openhtmltopdf.pdfbox)
            implementation(libs.openhtmltopdf.svg.support)
            implementation(libs.pdfbox)
            implementation(libs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test.junit)
        }

        jsMain.dependencies {
            implementation(npm("pdf-lib", "1.17.1"))
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(npm("pdf-lib", "1.17.1"))
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
        name = "CMP PDF Generator"
        description =
            "Cross-platform PDF generation library for Kotlin Multiplatform — " +
            "HTML, Markdown, and DSL input; File / ByteArray / URI / Share / Print / Save output. " +
            "Supports Android, iOS, macOS, JVM, JS."
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
