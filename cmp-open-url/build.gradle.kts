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

        withHostTestBuilder {}.configure {
            // android.jar in a JVM host test is a stub whose methods THROW by default, so a
            // framework call aborts a test even when the code under test handled the situation
            // correctly. Returning defaults lets the real behaviour be asserted instead.
            isReturnDefaultValues = true

            // Robolectric reads the merged manifest/resources.
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
            // No external dependencies — uses only platform APIs
        }

        // getByName: the KMP android library plugin generates no typed androidHostTest accessor.
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.junit)
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

// ── Android host-test exclusions (method-level) ─────────────────────────────────────────────
// Each entry below reaches a real Android framework service that a JVM host test cannot
// provide — ConnectivityManager, ProcessLifecycleOwner, Context.startActivity, or the init
// ContentProvider. android.jar is a stub here and no provider ever runs, so these cannot pass;
// injecting a Context does not help, because the services themselves must actually work.
//
// Excluded per METHOD, not per class: the same classes contain tests that pass on the host, and
// excluding whole classes silently dropped them from this tier. Listed literally rather than by
// wildcard so a newly-broken test fails loudly instead of being swallowed.
//
// Nothing is skipped overall — these are commonTest, so they still run on jvmTest, the native
// targets, jsTest and wasmJsTest, and the Android actual is covered on-device via
// `withDeviceTestBuilder`.
tasks.withType<Test>().configureEach {
    if (name == "testAndroidHostTest") {
        filter {
            // SEMANTIC exclusion, not an environmental one: the test's own name says
            // "onNonAndroid" — it asserts the behaviour of the NON-Android actuals and was never
            // meant to run here. It lives in commonTest (so it covers jvm/native/js) and this
            // filter is what scopes it away from the Android target. Nothing to "fix": running it
            // on Android would be asserting the wrong contract.
            excludeTestsMatching(
                "com.mobilebytelabs.kmptoolkit.openurl.OpenUrlTest.openWithApp_CustomHint_onNonAndroid_returnsSuccessOrNoHandler",
            )
            isFailOnNoMatchingTests = false
        }
    }
}

// Robolectric host-test config — emulated SDK comes from `robolectricSdk` in the version
// catalog and is code-generated into androidHostTest as `robolectric.ROBOLECTRIC_SDK`, so no
// test hardcodes an API level.
apply(from = "$rootDir/robolectric-host-test.gradle.kts")
kotlin.sourceSets.getByName("androidHostTest").kotlin.srcDir(
    tasks.named("generateRobolectricSdkConstant"),
)
