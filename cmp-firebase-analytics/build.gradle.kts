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
    applyDefaultHierarchyTemplate()

    // ── All 21 KMP targets — interface + Stub/NoOp/Test live in commonMain ──
    jvm()

    androidLibrary {
        namespace = "io.github.mobilebytelabs.kmptoolkit.analytics"
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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    watchosX64()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

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

    wasmJs {
        browser()
        nodejs()
    }

    // wasmWasi intentionally NOT declared:
    //   Ktor (JetBrains' own HTTP client), Kermit, and multiplatform-settings
    //   do not publish wasmWasi variants. Without HTTP/log/KV there is no
    //   transport for analytics. Re-add when the ecosystem catches up.

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ── Source set hierarchy ────────────────────────────────────────────────
    //
    //   commonMain (no GitLive — interface + types + Stub/NoOp/Test + DI factory)
    //       │
    //       ├── firebaseMain      — GitLive Firebase Analytics ships here (11 targets)
    //       │     ├── androidMain
    //       │     ├── jvmMain
    //       │     ├── iosMain     (iosX64 / iosArm64 / iosSimulatorArm64)
    //       │     ├── macosMain   (macosX64 / macosArm64)
    //       │     ├── tvosMain    (tvosX64 / tvosArm64 / tvosSimulatorArm64)
    //       │     └── jsMain      (browser + node)
    //       │
    //       └── nonFirebaseMain   — Measurement Protocol HTTP (9 targets)
    //             ├── watchosMain (watchosX64 / watchosArm64 / watchosSimulatorArm64 / watchosDeviceArm64)
    //             ├── linuxMain   (linuxX64 / linuxArm64)
    //             ├── mingwMain   (mingwX64)
    //             └── wasmJsMain
    //
    // wasmWasi target intentionally omitted — Ktor / Kermit / multiplatform-settings
    // do not publish wasmWasi variants. 20/21 targets supported.
    //
    // GitLive Firebase Analytics 2.4.x target matrix verified at:
    //   https://github.com/GitLiveApp/firebase-kotlin-sdk/blob/master/firebase-analytics/build.gradle.kts
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.multiplatform.settings)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        // ── Firebase tier — GitLive ships on these 11 targets ───────────────
        val firebaseMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.gitlive.firebase.analytics)
            }
        }
        // ── Non-Firebase tier — 9 targets where GitLive does NOT ship ───────
        // Get Firebase Measurement Protocol via HTTP. Same Firebase property,
        // same BigQuery dataset, just no native SDK features.
        val nonFirebaseMain by creating {
            dependsOn(commonMain.get())
        }

        // GitLive-supported → firebaseMain
        androidMain.get().dependsOn(firebaseMain)
        jvmMain.get().dependsOn(firebaseMain)
        iosMain.get().dependsOn(firebaseMain)
        macosMain.get().dependsOn(firebaseMain)
        tvosMain.get().dependsOn(firebaseMain)
        jsMain.get().dependsOn(firebaseMain)

        // Not supported by GitLive → nonFirebaseMain (Measurement Protocol HTTP)
        watchosMain.get().dependsOn(nonFirebaseMain)
        linuxMain.get().dependsOn(nonFirebaseMain)
        mingwMain.get().dependsOn(nonFirebaseMain)
        wasmJsMain.get().dependsOn(nonFirebaseMain)

        // ── Per-platform Ktor HTTP engine ──────────────────────────────────
        // commonMain only declares the client-core API; each platform binds
        // a concrete engine. Used by MeasurementProtocolAnalyticsHelper on
        // nonFirebaseMain platforms; firebaseMain platforms also have the
        // engine available so apps can opt to use MP everywhere.
        androidMain.dependencies {
            // GitLive's firebase-analytics-android pulls in com.google.firebase:firebase-analytics
            // and firebase-common WITHOUT versions — the BOM supplies them.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.ktor.client.cio)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        macosMain.dependencies { implementation(libs.ktor.client.darwin) }
        tvosMain.dependencies { implementation(libs.ktor.client.darwin) }
        watchosMain.dependencies { implementation(libs.ktor.client.darwin) }
        jsMain.dependencies { implementation(libs.ktor.client.js) }
        wasmJsMain.dependencies { implementation(libs.ktor.client.js) }
        linuxMain.dependencies { implementation(libs.ktor.client.curl) }
        mingwMain.dependencies { implementation(libs.ktor.client.winhttp) }
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
        name = "CMP Firebase Analytics"
        description =
            "Firebase Analytics for Kotlin Multiplatform — interface + Stub/NoOp/Test variants " +
            "across all 21 KMP targets, with GitLive Firebase Analytics on Android, iOS, " +
            "macOS, tvOS, JS, and JVM (11 targets)."
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
