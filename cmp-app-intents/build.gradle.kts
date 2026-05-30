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

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlinxSerialization)
    // v0.4 Phase 9 — ABI stability (kover deferred — incompatible with
    // the new `com.android.kotlin.multiplatform.library` plugin's
    // `androidLibrary {}` extension in Kover 0.9.1; re-enable when Kover ships support.)
    alias(libs.plugins.binaryCompatibilityValidator)
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-app-intents
// ============================================================================
// Declarative App Intents DSL for SiriKit Shortcuts + Spotlight (iOS 16+).
// Kotlin DSL emits a JSON manifest at runtime.
// iOS: ship CmpAppIntentBridge.swift + per-intent stub template (per Phase 0 TS2 —
// consumer copies the .swift file into their Xcode target; NO Package.swift / SPM).
// Android: on-device runtime registry + BroadcastReceiver (v0.1 scope — Google
// Assistant integration deferred to v0.2 cmp-app-intents-assistant per TS7).
// JVM Desktop + JS / wasmJs: no-op + invokeForTesting helper.
// watchOS: full impl via existing manifest + Swift bridge (watchOS 10+ Shortcuts).
// tvOS: Siri Suggestions via manifest; CoreSpotlight indexing skipped (iOS/macOS only).
// Linux / mingw: registry-only + manifest JSON to XDG / APPDATA dir.
// (wasmWasi excluded — no UI surface available.)
// iOS 16+ enforced via runtime if #available checks in shipped Swift code (TS1).
// v0.2 sub-plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/10-platform-parity-v0-2.md
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.mobilebytelabs.kmptoolkit.appintents"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        androidResources.enable = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    // tvOS targets (v0.2 — manifest only; Spotlight indexing skipped, no CoreSpotlight on tvOS)
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    // watchOS targets (v0.2 — FULL App Intents impl; watchOS 10+ Shortcuts via Swift bridge)
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    // Linux + mingw (v0.2 — registry-only; manifest JSON to XDG / APPDATA dir)
    linuxX64()
    linuxArm64()
    mingwX64()

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
            implementation(libs.kotlinx.serialization.json)
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
            implementation(libs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test.junit)
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING
// ============================================================================
mavenPublishing {
    signAllPublications()

    pom {
        name = "CMP App Intents"
        description =
            "Declarative App Intents DSL for Kotlin Multiplatform — " +
            "SiriKit Shortcuts + Spotlight (iOS 16+) via ship-source-file Swift bridge, " +
            "Android on-device runtime registry (Assistant integration deferred to v0.2), " +
            "JVM Desktop + JS/wasmJs no-op fallback. Per-intent declarative DSL with parameter types."
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

// ============================================================================
// v0.4 Phase 5 — Gradle codegen tasks (closes ADR-09 #12)
// ============================================================================
// `generateShortcutsXml` emits res/xml/cmp_app_intents_shortcuts.xml from a consumer-emitted
// manifest JSON. `generateSwiftIntents` emits per-intent Swift @AppIntent struct stubs.
// Both use MANIFEST-JSON approach per Phase 0 S1.D verdict (no KSP dep).
//
// Consumer wiring (in their build.gradle.kts):
//   cmpAppIntents {
//       generateShortcuts = true
//       shortcutsOutputDir = file("src/main/res/xml")
//       generateSwift = true
//       swiftOutputDir = file("../iosApp/iosApp/AppIntents/Generated")
//   }
//
// Consumer must emit ${buildDir}/cmp-app-intents/manifest.json at build time via small
// bootstrap task that calls AppIntentsConfig.serializeManifest() on the registered config.

interface CmpAppIntentsExtension {
    var generateShortcuts: Boolean
    var shortcutsOutputDir: java.io.File?
    var generateSwift: Boolean
    var swiftOutputDir: java.io.File?
}

extensions.create("cmpAppIntents", CmpAppIntentsExtension::class.java).apply {
    generateShortcuts = false
    shortcutsOutputDir = null
    generateSwift = false
    swiftOutputDir = null
}

tasks.register("generateShortcutsXml") {
    group = "cmp-app-intents"
    description = "Emit res/xml/cmp_app_intents_shortcuts.xml from consumer-emitted manifest JSON (v0.4 codegen)"
    doLast {
        val ext = extensions.getByType(CmpAppIntentsExtension::class.java)
        if (!ext.generateShortcuts) {
            logger.lifecycle("cmpAppIntents.generateShortcuts = false — skipping")
            return@doLast
        }
        val outDir =
            ext.shortcutsOutputDir
                ?: error("cmpAppIntents.shortcutsOutputDir is required when generateShortcuts = true")
        val manifestJson = file("${layout.buildDirectory.get()}/cmp-app-intents/manifest.json")
        if (!manifestJson.exists()) {
            logger.warn(
                "cmp-app-intents manifest.json not found at ${manifestJson.absolutePath} — consumer must emit it via bootstrap task",
            )
            return@doLast
        }
        outDir.mkdirs()
        // Inline minimal JSON → XML transform. Real consumer pipeline would use the
        // serializeManifest()-emitted JSON shape (see cmp-app-intents/src/commonMain/.../AppIntents.kt
        // ManifestEntry data class). For v0.4 codegen, the XML structure is:
        //   <shortcuts><capability android:name="actions.intent.{BII}">...</capability>...</shortcuts>
        // where BII defaults to OPEN_APP_FEATURE per AssistantBii.resolveBii() unless overridden.
        val xml =
            buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("""<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">""")
                // Naive JSON parse — for production, consumer-emitted JSON is structured per ManifestEntry
                val json = manifestJson.readText()
                // Extract intent ids via regex (the JSON shape has `"id":"..."` per entry)
                Regex(""""id"\s*:\s*"([^"]+)"""").findAll(json).forEach { match ->
                    val intentId = match.groupValues[1]
                    appendLine("""  <capability android:name="actions.intent.OPEN_APP_FEATURE">""")
                    appendLine("""    <intent android:action="android.intent.action.VIEW">""")
                    appendLine("""      <url-template android:value="cmp-app-intent://$intentId" />""")
                    appendLine("""    </intent>""")
                    appendLine("""  </capability>""")
                }
                appendLine("""</shortcuts>""")
            }
        outDir.resolve("cmp_app_intents_shortcuts.xml").writeText(xml)
        logger.lifecycle("Wrote ${outDir.resolve("cmp_app_intents_shortcuts.xml")}")
    }
}

tasks.register("generateSwiftIntents") {
    group = "cmp-app-intents"
    description = "Emit per-intent Swift @AppIntent stubs from consumer-emitted manifest JSON (v0.4 codegen)"
    doLast {
        val ext = extensions.getByType(CmpAppIntentsExtension::class.java)
        if (!ext.generateSwift) {
            logger.lifecycle("cmpAppIntents.generateSwift = false — skipping")
            return@doLast
        }
        val outDir =
            ext.swiftOutputDir
                ?: error("cmpAppIntents.swiftOutputDir is required when generateSwift = true")
        val manifestJson = file("${layout.buildDirectory.get()}/cmp-app-intents/manifest.json")
        if (!manifestJson.exists()) {
            logger.warn(
                "cmp-app-intents manifest.json not found at ${manifestJson.absolutePath} — consumer must emit it via bootstrap task",
            )
            return@doLast
        }
        outDir.mkdirs()
        val templatePath = file("swift/templates/AppIntentStub.swift.template")
        val template =
            if (templatePath.exists()) {
                templatePath.readText()
            } else {
                """
            |import AppIntents
            |
            |@available(iOS 16.0, macOS 13.0, watchOS 10.0, *)
            |struct ${'$'}{INTENT_ID}AppIntent: AppIntent {
            |    static var title: LocalizedStringResource = "${'$'}{INTENT_TITLE}"
            |    static var description = IntentDescription("${'$'}{INTENT_DESCRIPTION}")
            |
            |    @MainActor
            |    func perform() async throws -> some IntentResult {
            |        await CmpAppIntentBridge.shared.perform(id: "${'$'}{INTENT_ID}", params: [:])
            |        return .result()
            |    }
            |}
                """.trimMargin()
            }
        val json = manifestJson.readText()
        var emitted = 0
        Regex(""""id"\s*:\s*"([^"]+)"\s*,\s*"title"\s*:\s*"([^"]+)"\s*,\s*"description"\s*:\s*"([^"]+)"""")
            .findAll(json)
            .forEach { match ->
                val (intentId, title, description) = match.destructured
                val capitalized = intentId.replaceFirstChar { it.uppercase() }
                val body =
                    template
                        .replace("\${INTENT_ID}", capitalized)
                        .replace("\${INTENT_TITLE}", title)
                        .replace("\${INTENT_DESCRIPTION}", description)
                outDir.resolve("${capitalized}AppIntent.swift").writeText(body)
                emitted++
            }
        logger.lifecycle("Wrote $emitted Swift @AppIntent stub(s) to $outDir")
    }
}

// Library Runtime Observability — auto-generate CmpMetadata.kt for cmp-observe hooks (epic 2026-05-30)
apply(from = "$rootDir/cmp-observe-metadata.gradle.kts")
