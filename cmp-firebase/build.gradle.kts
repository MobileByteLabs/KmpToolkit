import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinSerialization)
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
        namespace = "io.github.mobilebytelabs.kmptoolkit.firebase"
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
    //       │     ├── iosMain     (iosX64 / iosArm64 / iosSimulatorArm64)
    //       │     ├── macosMain   (macosX64 / macosArm64)
    //       │     ├── tvosMain    (tvosX64 / tvosArm64 / tvosSimulatorArm64)
    //       │     ├── jsMain      (browser + node)
    //       │     └── wasmJsMain  (GitLive 3.0.0-alpha02+ — JS-parity wasmJs target)
    //       │
    //       └── nonFirebaseMain   — Measurement Protocol HTTP (4 targets)
    //             ├── jvmMain
    //             ├── linuxMain   (linuxX64 / linuxArm64)
    //             └── mingwMain   (mingwX64)
    //
    // wasmWasi + watchOS intentionally omitted — cmp-network-monitor (a dependency) and
    // Ktor/Kermit/multiplatform-settings do not publish those variants. 15 targets supported.
    //
    // GitLive Firebase Analytics 3.0.x target matrix verified at:
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
            // Optional wiring: the app opts into network telemetry / offline queue by passing
            // its NetworkMonitor to attachNetworkTelemetry(...) / OfflineEventQueue(...).
            // `api` so the NetworkMonitor type is visible at those call sites.
            api(project(":cmp-network-monitor"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        // ── Firebase tier — GitLive ships on these 11 targets ───────────────
        val firebaseMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.gitlive.firebase.app)
                api(libs.gitlive.firebase.analytics)
            }
        }
        // ── Non-Firebase tier — 9 targets where GitLive does NOT ship ───────
        // Get Firebase Measurement Protocol via HTTP. Same Firebase property,
        // same BigQuery dataset, just no native SDK features.
        val nonFirebaseMain by creating {
            dependsOn(commonMain.get())
        }

        // GitLive-supported native analytics + programmatic init → firebaseMain
        androidMain.get().dependsOn(firebaseMain)
        iosMain.get().dependsOn(firebaseMain)
        macosMain.get().dependsOn(firebaseMain)
        tvosMain.get().dependsOn(firebaseMain)
        jsMain.get().dependsOn(firebaseMain)
        // wasmJs joined the GitLive-supported set in 3.0.0-alpha02 (upstream PR #832,
        // full parity with the JS target). Promoted off the Measurement-Protocol tier.
        // NOTE: firebase-crashlytics did NOT gain wasmjs — the crashlytics tier below
        // deliberately keeps wasmJs on crashlyticsFallbackMain.
        wasmJsMain.get().dependsOn(firebaseMain)

        // Not supported by GitLive → nonFirebaseMain (Measurement Protocol HTTP).
        // JVM is here too: GitLive's JVM analytics is a no-op stub AND JVM has no
        // Android Context for programmatic init, so JVM runs on the MP tier
        // (no-op platformInitializeFirebase + MP analytics).
        jvmMain.get().dependsOn(nonFirebaseMain)
        linuxMain.get().dependsOn(nonFirebaseMain)
        mingwMain.get().dependsOn(nonFirebaseMain)

        // ── Crashlytics tier ────────────────────────────────────────────────
        //
        // GitLive Crashlytics ships on a DIFFERENT, smaller matrix than Analytics
        // (no JVM, no JS) — so it gets its own source-set split, orthogonal to the
        // firebaseMain/nonFirebaseMain analytics split above. Each leaf target
        // dependsOn exactly one crashlytics tier + one analytics tier.
        //
        //   crashlyticsFirebaseMain (6)  — GitLive Crashlytics: android, ios×3, macos×2
        //   crashlyticsFallbackMain (9)  — LoggingCrashReporter: jvm, js, tvos×3, linux×2, mingw, wasmJs
        //
        // GitLive firebase-crashlytics 3.0.0-alpha02 declares
        //   firebase-crashlytics.supportedTargets = ios, macos, jvm, android
        // (verified in gradle.properties at the v3.0.0-alpha02 tag) — NO tvOS / JS /
        // wasmJs / watchOS / linux / mingw.
        //
        // NOTE alpha02 added `wasmjs` to EVERY other GitLive module but deliberately NOT
        // to crashlytics, which is why wasmJs is promoted to firebaseMain for ANALYTICS
        // above yet stays on crashlyticsFallbackMain here.
        //
        // JVM is listed upstream but kept on the fallback tier anyway: GitLive's JVM
        // crashlytics is a stub — the same reason jvm sits on nonFirebaseMain for
        // analytics.
        // Crashlytics also has no ingestion REST API, so the fallback is a structured
        // Kermit-logged CrashReport (AI-feedable), not a Measurement-Protocol HTTP.
        val crashlyticsFirebaseMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.gitlive.firebase.crashlytics)
            }
        }
        val crashlyticsFallbackMain by creating {
            dependsOn(commonMain.get())
        }
        // GitLive Crashlytics-supported → crashlyticsFirebaseMain
        androidMain.get().dependsOn(crashlyticsFirebaseMain)
        iosMain.get().dependsOn(crashlyticsFirebaseMain)
        macosMain.get().dependsOn(crashlyticsFirebaseMain)
        // Not supported by GitLive Crashlytics → crashlyticsFallbackMain
        jvmMain.get().dependsOn(crashlyticsFallbackMain)
        jsMain.get().dependsOn(crashlyticsFallbackMain)
        tvosMain.get().dependsOn(crashlyticsFallbackMain)
        linuxMain.get().dependsOn(crashlyticsFallbackMain)
        mingwMain.get().dependsOn(crashlyticsFallbackMain)
        wasmJsMain.get().dependsOn(crashlyticsFallbackMain)

        // ── Per-platform Ktor HTTP engine ──────────────────────────────────
        // commonMain only declares the client-core API; each platform binds
        // a concrete engine. Used by MeasurementProtocolAnalyticsHelper on
        // nonFirebaseMain platforms; firebaseMain platforms also have the
        // engine available so apps can opt to use MP everywhere.
        androidMain.dependencies {
            // GitLive's firebase-analytics-android + firebase-crashlytics-android pull in
            // com.google.firebase:firebase-{analytics,crashlytics} + firebase-common WITHOUT
            // versions — the BOM supplies them. `api` (not implementation) so CONSUMERS inherit
            // the BOM constraint too; otherwise a consumer's androidCompileClasspath cannot
            // resolve the unversioned transitive Firebase Android artifacts (empty-version error).
            api(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.ktor.client.cio)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        macosMain.dependencies { implementation(libs.ktor.client.darwin) }
        tvosMain.dependencies { implementation(libs.ktor.client.darwin) }
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
        name = "CMP Firebase"
        description =
            "Firebase for Kotlin Multiplatform — Analytics + Crashlytics with a single in-library " +
            "setup surface (FirebaseKit.initialize + Android auto-init). GitLive Firebase Analytics " +
            "on 10 targets (Measurement-Protocol HTTP fallback on the rest) and GitLive Crashlytics " +
            "on 6 targets, with an AI-feedable structured CrashReport fallback (Kermit) everywhere else. " +
            "Interface + Stub/NoOp/Test variants across all 15 supported KMP targets."
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
