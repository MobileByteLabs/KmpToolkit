import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

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
        namespace = "io.github.mobilebytelabs.kmptoolkit.openurl"
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
            // No external dependencies — uses only platform APIs
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            // no extra deps — Intent/PackageManager/Uri are in the Android SDK
        }
    }
}

mavenPublishing {
    signAllPublications()

    pom {
        name = "CMP Open URL"
        description =
            "Cross-platform URL opening for Kotlin Multiplatform — browser, email, maps, phone, SMS, and custom URI schemes"
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

// Library Runtime Observability — auto-generate CmpMetadata.kt for cmp-observe hooks (epic 2026-05-30)
apply(from = "$rootDir/cmp-observe-metadata.gradle.kts")
