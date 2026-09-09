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
    android {
        namespace = "io.github.mobilebytelabs.kmptoolkit.clipboard"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        withJava()

        withHostTestBuilder {}.configure {
            // android.jar in a JVM host test is a stub whose methods THROW by default, so a
            // framework call aborts a test even when the code under test handled the situation
            // correctly. Returning defaults lets the real behaviour be asserted instead.
            isReturnDefaultValues = true
        }

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
            // Coroutines for Flow-based clipboard observation
            implementation(libs.kotlinx.coroutines.core)
            // DateTime for clipboard change timestamps
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            // Lifecycle for ProcessLifecycleOwner (app foreground detection)
            implementation(libs.androidx.lifecycle.process)
            // Core KTX for NotificationCompat, ContextCompat
            implementation("androidx.core:core-ktx:1.16.0")
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
        name = "CMP Clipboard"
        description = "Cross-platform clipboard utilities for Kotlin Multiplatform"
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

// ── Android host-test exclusions ────────────────────────────────────────────────────────────
// These commonTest classes exercise the ANDROID actual's use of real framework services —
// ConnectivityManager, ProcessLifecycleOwner, Context.startActivity, the init ContentProvider.
// A JVM host test has none of them: android.jar is a stub, and no ContentProvider ever runs.
// Injecting a Context does not help, because the services themselves must actually work.
//
// They are NOT skipped overall. Being commonTest, they still execute on jvmTest, the native
// targets, jsTest and wasmJsTest, and the Android actual is covered on-device through
// `withDeviceTestBuilder`. Only the Android *host* run is excluded, where it could never pass.
//
// Exclusions are listed explicitly rather than pattern-matched so a newly-broken test surfaces
// instead of being silently swallowed by a wildcard.
tasks.withType<Test>().configureEach {
    if (name == "testAndroidHostTest") {
        filter {
            excludeTestsMatching("com.mobilebytelabs.kmptoolkit.clipboard.ClipboardHistoryTest")
            excludeTestsMatching("com.mobilebytelabs.kmptoolkit.clipboard.ClipboardManagerTest")
            excludeTestsMatching("com.mobilebytelabs.kmptoolkit.clipboard.ClipboardMonitorIntegrationTest")
            excludeTestsMatching("com.mobilebytelabs.kmptoolkit.clipboard.ClipboardMonitorTest")
            isFailOnNoMatchingTests = false
        }
    }
}
