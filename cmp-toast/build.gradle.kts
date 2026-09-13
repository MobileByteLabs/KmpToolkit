import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
    alias(libs.plugins.binaryCompatibilityValidator)
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
        namespace = "io.github.mobilebytelabs.kmptoolkit.toast"
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

    // ========================================================================
    // iOS Targets
    // ========================================================================
    iosArm64()
    iosSimulatorArm64()

    // ========================================================================
    // macOS Targets
    // ========================================================================
    macosArm64()

    // ========================================================================
    // Web Targets (added 2026-05-30 — library-runtime-observability audit follow-up)
    // Compose Multiplatform Web supports both JS/IR + WasmJS browser runtimes.
    // Toast has zero expect declarations → adding the targets requires no per-
    // platform actuals; the Compose-common `Snackbar` / `Toast` composables compile
    // straight through to web.
    //
    // Compose Multiplatform does NOT support tvOS / watchOS / Linux / mingw —
    // those targets remain out of scope until upstream Compose adds them.
    // ========================================================================
    js(IR) {
        browser()
    }
    wasmJs {
        browser()
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
        // koin-core publishes for every target this module builds (Compose targets only, so no
        // wasmWasi to worry about here), but the split is kept consistent with the rest of the
        // toolkit so a reader finds the Koin binding in the same place in every module.
        val koinMain = create("koinMain").apply { dependsOn(getByName("commonMain")) }
        val koinTest = create("koinTest").apply { dependsOn(getByName("commonTest")) }
        listOf("jvmMain", "androidMain", "appleMain", "jsMain", "wasmJsMain")
            .forEach { getByName(it).dependsOn(koinMain) }
        listOf("jvmTest", "appleTest", "jsTest", "wasmJsTest")
            .forEach { getByName(it).dependsOn(koinTest) }

        koinMain.dependencies {
            implementation(libs.koin.core)
        }

        commonMain.dependencies {

            // Reports this library's lifecycle to hooks the consumer registered. Pure stdlib since
            // the Firebase hooks moved to cmp-observe-firebase — 2 classpath lines, no transitive SDK.
            implementation(project(":cmp-observe"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlin.test)
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
        name = "CMP Toast"
        description = "Cross-platform toast/snackbar for Compose Multiplatform"
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
