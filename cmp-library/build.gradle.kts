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
}

// ============================================================================
// TEMPLATE LIBRARY CONFIGURATION
// ============================================================================
// This module serves as a template/reference for creating new KMP libraries.
// Copy this module structure when adding new library modules.
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
        namespace = "io.github.mobilebytelabs.kmptoolkit"
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
    // watchOS Targets
    // ========================================================================
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

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
        commonMain.dependencies {
            // Add your common dependencies here
            // implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // Platform-specific dependencies example:
        // androidMain.dependencies {
        //     implementation(libs.some.android.library)
        // }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING CONFIGURATION
// ============================================================================
// NOTE: This is a template module. Update coordinates and pom info when
// copying this module to create a new library.
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
        name = "KMP Template Library"
        description =
            "Template module for creating new KMP libraries - demonstrates expect/actual pattern and multiplatform setup"
        inceptionYear = "2025"
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
