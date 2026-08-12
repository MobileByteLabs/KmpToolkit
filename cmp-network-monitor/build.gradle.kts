import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
}

// ============================================================================
// LIBRARY CONFIGURATION
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    // Apply default hierarchy template for automatic source set setup
    applyDefaultHierarchyTemplate()

    // ========================================================================
    // JVM Target
    // ========================================================================
    jvm()

    // ========================================================================
    // Android Target
    // ========================================================================
    androidLibrary {
        namespace = "io.github.mobilebytelabs.kmptoolkit.networkmonitor"
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
    }

    // ========================================================================
    // iOS Targets
    // ========================================================================
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // ========================================================================
    // macOS Targets
    // ========================================================================
    macosX64()
    macosArm64()

    // ========================================================================
    // tvOS Targets
    // ========================================================================
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    // ========================================================================
    // watchOS Targets (arm32 removed — deprecated by Kotlin, Apple requires 64-bit since watchOS 7)
    // ========================================================================

    // ========================================================================
    // Linux Targets
    // ========================================================================
    linuxX64()
    linuxArm64()

    // ========================================================================
    // Windows Target
    // ========================================================================
    mingwX64()

    // ========================================================================
    // JavaScript Target
    // ========================================================================
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }

    // ========================================================================
    // WebAssembly Targets
    // ========================================================================
    wasmJs {
        browser()
        nodejs()
    }

    wasmWasi {
        nodejs()
    }

    // ========================================================================
    // Compiler Options
    // ========================================================================
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ========================================================================
    // Source Sets Configuration
    // ========================================================================
    sourceSets {
        // appleMain is already created by applyDefaultHierarchyTemplate() in Kotlin 2.3.0
        // All Apple targets (iOS, macOS, tvOS, watchOS) share code via appleMain automatically

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // NOTE: cmp-observe is intentionally NOT in commonMain.
            // cmp-observe ships only 7 of the 11 targets this module supports
            // (constrained by GitLive Firebase + Ktor — no tvos/watchos/linux/mingw).
            // The LibraryObservation.notifyInit call is wired per-source-set, in each
            // platform that actually has an init path (currently only androidMain).
            //
            // Per library-runtime-observability epic Phase 02 T6 post-fix (2026-05-30).
            // Audit: cmp-observe target gap vs cmp-network-monitor → 4 missing targets
            // (tvos, watchos, linux, mingw); putting the dep in commonMain would
            // require cmp-observe to ship for those targets too.
        }

        // Android-only cmp-observe dep — NetworkMonitorInitProvider's ContentProvider
        // is androidMain-scoped; this is the only source-set that imports cmp-observe.
        // Other platform init paths (jvmMain/iosMain factories — future) should add
        // their own per-source-set dep when they add notifyInit calls.
        androidMain.dependencies {
            implementation(project(":cmp-observe"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.app.cash.turbine)
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING CONFIGURATION
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
        name = "CMP Network Monitor"
        description =
            "Reactive network connectivity monitoring for Kotlin Multiplatform — StateFlow-based, all 21 KMP targets"
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
