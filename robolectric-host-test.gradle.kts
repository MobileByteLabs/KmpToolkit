/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

// Robolectric host-test configuration — ONE source of truth for the emulated API level.
//
// Apply from any module whose androidHostTest runs under Robolectric:
//     apply(from = "$rootDir/robolectric-host-test.gradle.kts")
//
// WHY THIS EXISTS
// The emulated SDK was previously hardcoded as `private const val ANDROID_HOST_TEST_SDK = 35`
// (and, in one file, ROBOLECTRIC_MAX_SDK) repeated across four test classes in three modules.
// Four copies of a number that must move together is drift waiting to happen: bump Robolectric,
// miss one file, and that module silently keeps emulating the old API.
//
// This generates a single `RobolectricSdk.kt` constant per module from `robolectricSdk` in
// gradle/libs.versions.toml, which tests reference as `@Config(sdk = [ROBOLECTRIC_SDK])`.
//
// A generated `robolectric.properties` on the test classpath would need no annotation at all and
// was tried first — but AGP assigns the androidHostTest classpath after this script's
// `configureEach` runs, so the file never reached Robolectric and every class failed with
// "Package targetSdkVersion=37 > maxSdkVersion=35". A compile-time constant is wired through the
// Kotlin source set instead, which is deterministic and matches how :cmp-firebase-gradle-plugin
// already generates its version constant.
//
// WHY THE VERSION IS PINNED BELOW compileSdk
// The toolkit compiles against SDK 37; Robolectric ships emulation images only to 35 and
// hard-fails above it. These suites assert connectivity, lifecycle and composition behaviour,
// none of which is API-level sensitive, so emulating 35 costs nothing. Raise `robolectricSdk` in
// the catalog the moment Robolectric ships a 37 image — one edit, every module follows.

/**
 * Reads a version from gradle/libs.versions.toml directly.
 *
 * The `libs` type-safe accessor is not available inside a script applied with `apply(from = …)`,
 * and duplicating the value here would defeat the whole point of this file.
 */
fun catalogVersion(key: String): String =
    rootProject
        .file("gradle/libs.versions.toml")
        .readLines()
        .firstOrNull { it.trimStart().startsWith("$key ") || it.trimStart().startsWith("$key=") }
        ?.substringAfter('=')
        ?.trim()
        ?.trim('"')
        ?: error("$key not set in gradle/libs.versions.toml")

val generateRobolectricSdkConstant =
    tasks.register("generateRobolectricSdkConstant") {
        val sdk = catalogVersion("robolectricSdk")
        val outputDir = layout.buildDirectory.dir("generated/robolectric/kotlin")
        inputs.property("sdk", sdk)
        outputs.dir(outputDir)
        doLast {
            val pkgDir = outputDir.get().asFile.resolve("robolectric")
            pkgDir.mkdirs()
            pkgDir.resolve("RobolectricSdk.kt").writeText(
                """
                |// Generated from `robolectricSdk` in gradle/libs.versions.toml — do not edit.
                |package robolectric
                |
                |/** Emulated API level for Robolectric host tests. */
                |const val ROBOLECTRIC_SDK: Int = $sdk
                |
                """.trimMargin(),
            )
        }
    }

// Deliberately NOT wired to the source set here: an `apply(from = …)` script has no Kotlin
// Gradle Plugin types on its classpath, so `kotlin { sourceSets … }` does not resolve. Each
// consuming module adds the one-line srcDir wiring right after applying this file.
