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
    alias(libs.plugins.binaryCompatibilityValidator)
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
        namespace = "io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose"
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

            // Robolectric reads the MERGED manifest/resources; without this the
            // ui-test-manifest activity that Compose's ActivityScenario launches is invisible
            // and every UI test dies with "Unable to resolve activity for Intent { MAIN }".
            isIncludeAndroidResources = true
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
    iosArm64()
    iosSimulatorArm64()

    // ========================================================================
    // macOS Targets
    // ========================================================================
    macosArm64()

    // ========================================================================
    // JavaScript Target
    // ========================================================================
    js {
        browser()
    }

    // ========================================================================
    // WebAssembly Target
    // ========================================================================
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
        commonMain.dependencies {
            implementation(project(":cmp-network-monitor"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // Real composition testing: renders the composables and asserts what a user sees,
            // rather than only asserting that the CompositionLocal object is non-null.
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }

        // getByName: the new com.android.kotlin.multiplatform.library plugin does not generate a
        // typed `androidHostTest` accessor the way it does for commonTest/jvmTest.
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.junit)
            implementation(libs.androidx.compose.ui.test.manifest)
        }

        jvmTest.dependencies {
            // Skiko's native backend, required for runComposeUiTest on the JVM/desktop target —
            // without it every composition fails with
            // "NoClassDefFoundError: Could not initialize class org.jetbrains.skia.Surface".
            // JVM-only on purpose: the other targets bring their own renderer.
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.test)
        }

        // getByName: the new com.android.kotlin.multiplatform.library plugin does not generate a
        // typed `androidHostTest` accessor the way it does for commonTest/jvmTest.
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.junit)
            implementation(libs.androidx.compose.ui.test.manifest)
        }

        jvmTest.dependencies {
            // Skiko's native backend, required for runComposeUiTest on the JVM/desktop target —
            // without it every composition fails with
            // "NoClassDefFoundError: Could not initialize class org.jetbrains.skia.Surface".
            // JVM-only on purpose: the other targets bring their own renderer.
            implementation(compose.desktop.currentOs)
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
        name = "CMP Network Monitor Compose"
        description =
            "Compose Multiplatform extensions for cmp-network-monitor — rememberNetworkMonitor, NetworkAwareContent, ConnectivityBanner"
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

// Robolectric host-test config — emulated SDK comes from `robolectricSdk` in the version
// catalog and is code-generated into androidHostTest as `robolectric.ROBOLECTRIC_SDK`, so no
// test hardcodes an API level.
apply(from = "$rootDir/robolectric-host-test.gradle.kts")
kotlin.sourceSets.getByName("androidHostTest").kotlin.srcDir(
    tasks.named("generateRobolectricSdkConstant"),
)
