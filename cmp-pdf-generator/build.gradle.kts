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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
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

    androidLibrary {
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
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.html)
            implementation(libs.markdown)
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
