/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

// ============================================================================
// cmp-observe-metadata.gradle.kts — shared Gradle task generating CmpMetadata.kt
// from CMP_LIBRARY_METADATA.gradle's `extra["..."]` properties at compile time.
//
// Authored 2026-05-30 by library-runtime-observability epic Phase 02 T3 (AC #10).
//
// Each cmp-* module that consumes cmp-observe applies this script:
//
//     // In cmp-share/build.gradle.kts (or any cmp-*):
//     apply(from = "$rootDir/cmp-observe-metadata.gradle.kts")
//
// At every `:cmp-share:compileKotlinCommon`, generates:
//
//     cmp-share/build/generated/observability/io/github/mobilebytelabs/kmptoolkit/share/CmpMetadata.kt
//
// containing compile-time constants:
//
//     internal object CmpMetadata {
//         const val NAME = "cmp-share"
//         const val VERSION = "3.2.11"
//         const val ARTIFACT = "io.github.mobilebytelabs:cmp-share"
//     }
//
// plus a `cmpMetadata()` factory returning cmp-observe's CmpMetadata, so a module reports itself
// with `observeInit(cmpMetadata()) { … }` and never repeats the three-field construction.
//
// REQUIRES the applying module to depend on cmp-observe in commonMain. That is possible for every
// module as of 2026-09-13, when cmp-observe reached the full 21-target matrix; before that it
// shipped 15 and a commonMain dependency would have capped its consumers.
//
// The generated file is automatically added to commonMain.kotlin.srcDirs.
// ============================================================================

// Read CMP_LIBRARY_METADATA.gradle if it exists (top-level umbrella defaults).
val metadataFile = rootProject.file("CMP_LIBRARY_METADATA.gradle.kts")
if (metadataFile.exists()) {
    apply(from = metadataFile)
}

// Module name = the project's directory name (cmp-share, cmp-network-monitor, etc.)
val moduleName: String = project.name
val moduleKey: String = moduleName.replace('-', '_')

// Resolve version + artifact from rootProject.extra; fall back to UNKNOWN.
val moduleVersion: String =
    try {
        rootProject.extra["${moduleKey}_version"] as String
    } catch (_: Exception) {
        "UNKNOWN"
    }
val moduleArtifact: String =
    try {
        rootProject.extra["${moduleKey}_artifact"] as String
    } catch (_: Exception) {
        "io.github.mobilebytelabs:$moduleName"
    }

// Kotlin package for the generated file — DETECTED from the module's own sources, not assumed.
//
// This was previously hardcoded to `io.github.mobilebytelabs.kmptoolkit.<short>`, but the toolkit uses
// TWO package roots: `io.github.mobilebytelabs.kmptoolkit.*` (cmp-network-monitor, cmp-observe) and
// `com.mobilebytelabs.kmptoolkit.*` (cmp-share, cmp-open-url, cmp-clipboard, cmp-app-review, …).
// For every module in the second group the generated CmpMetadata landed in a package its own source
// could not see, so using it meant an explicit cross-package import of an `internal` declaration.
//
// That is the likeliest reason this integration sat at ONE module for four months: cmp-network-monitor
// happens to be in the group the hardcoded guess matched, so it worked there and was friction
// everywhere else. Reading the real package removes the trap rather than documenting it.
val modulePackage: String =
    run {
        val srcRoot = project.file("src/commonMain/kotlin")
        val declared =
            srcRoot
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .mapNotNull { file ->
                    file.useLines { lines ->
                        lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim()
                    }
                }.distinct()
                .toList()

        // The module root is the longest package prefix COMMON to every declared package, compared
        // segment-wise. Previously this took the shortest declared package, which silently assumes
        // some file sits at the root: cmp-product-tickets has none (its files are all in .config,
        // .di, .data.remote, .domain.model), so the shortest was `…producttickets.di` and the
        // generated file landed inside the DI subpackage — invisible to every other file in the
        // module. Segment-wise also matters: comparing by character length alone can pick a longer
        // sibling over the true parent.
        val root =
            declared
                .map { it.split('.') }
                .reduceOrNull { a, b -> a.zip(b).takeWhile { (x, y) -> x == y }.map { it.first } }
                ?.joinToString(".")
                ?.takeIf { it.isNotEmpty() }

        root ?: "io.github.mobilebytelabs.kmptoolkit.${moduleName.removePrefix("cmp-").replace("-", "")}"
    }

abstract class CmpMetadataGenTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Input
    abstract val moduleName: org.gradle.api.provider.Property<String>

    @get:org.gradle.api.tasks.Input
    abstract val moduleVersion: org.gradle.api.provider.Property<String>

    @get:org.gradle.api.tasks.Input
    abstract val moduleArtifact: org.gradle.api.provider.Property<String>

    @get:org.gradle.api.tasks.Input
    abstract val modulePackage: org.gradle.api.provider.Property<String>

    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDir: org.gradle.api.file.DirectoryProperty

    /**
     * This script itself, as a task input.
     *
     * Without it the declared inputs are only the module's name/version/artifact/package, none of
     * which change when the GENERATOR changes — so every module whose metadata was already
     * generated stayed UP-TO-DATE and kept emitting the old file. That is how cmp-network-monitor
     * ended up with a `CmpMetadata.kt` lacking the `cmpMetadata()` factory long after the factory
     * was added here, failing compilation with "Unresolved reference 'cmpMetadata'" while every
     * freshly-built module had it. PathSensitivity.NONE: the content matters, the location does not.
     */
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
    abstract val generatorScript: org.gradle.api.file.RegularFileProperty

    /**
     * Whether the applying module actually depends on cmp-observe.
     *
     * The generated file has two halves with different requirements: the constants object needs
     * nothing, while `cmpMetadata()` returns cmp-observe's type and therefore needs it on the
     * commonMain classpath. Emitting both unconditionally made this script uncompilable in any
     * module without that dependency — which is 21 of the 22 that apply it.
     */
    @get:org.gradle.api.tasks.Input
    abstract val hasObserveDependency: org.gradle.api.provider.Property<Boolean>

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val pkgDir = outputDir.get().asFile.resolve(modulePackage.get().replace('.', '/'))
        pkgDir.mkdirs()
        // The convenience accessor is emitted ONLY where its type can resolve. A module without
        // cmp-observe still gets NAME/VERSION/ARTIFACT, which is what most callers actually read.
        val observeAccessor =
            if (hasObserveDependency.getOrElse(false)) {
                """

                /**
                 * This module's identity, as the type cmp-observe's hooks receive.
                 *
                 * Generated so a call site is `observeInit(cmpMetadata()) { … }` rather than the
                 * three-field construction plus an `import … as ObserveMetadata` alias every module
                 * would otherwise need — the two `CmpMetadata` names (this object and cmp-observe's
                 * data class) collide, and aliasing at 22 call sites is how the integration stayed at
                 * one module for four months.
                 */
                internal fun cmpMetadata(): io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata =
                    io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata(
                        name = CmpMetadata.NAME,
                        version = CmpMetadata.VERSION,
                        artifact = CmpMetadata.ARTIFACT,
                    )
                """.trimIndent()
            } else {
                ""
            }
        pkgDir.resolve("CmpMetadata.kt").writeText(
            """
            /*
             * Auto-generated by cmp-observe-metadata.gradle.kts. DO NOT EDIT.
             *
             * Source: CMP_LIBRARY_METADATA.gradle (regenerated post-publish by mbl-actionhub).
             * Per library-runtime-observability epic Phase 02 T4 (AC #10).
             */
            package ${modulePackage.get()}

            internal object CmpMetadata {
                const val NAME: String = "${moduleName.get()}"
                const val VERSION: String = "${moduleVersion.get()}"
                const val ARTIFACT: String = "${moduleArtifact.get()}"
            }

            $observeAccessor
            """.trimIndent(),
        )
    }
}

// Capture script-level vars before they are shadowed by the task's same-named abstract properties.
val capturedModuleName = moduleName
val capturedModuleVersion = moduleVersion
val capturedModuleArtifact = moduleArtifact
val capturedModulePackage = modulePackage

val genTask =
    tasks.register<CmpMetadataGenTask>("generateCmpMetadata") {
        moduleName.set(capturedModuleName)
        moduleVersion.set(capturedModuleVersion)
        moduleArtifact.set(capturedModuleArtifact)
        modulePackage.set(capturedModulePackage)
        outputDir.set(layout.buildDirectory.dir("generated/observability"))
        generatorScript.set(rootProject.file("cmp-observe-metadata.gradle.kts"))
        // Detected from the module's own declared dependencies rather than assumed. `provider {}`
        // defers resolution until execution, so this reads the configuration AFTER the module's
        // build script has finished declaring it.
        hasObserveDependency.set(
            provider {
                configurations.names
                    .filter { it.contains("ommonMain", ignoreCase = false) || it.startsWith("common") }
                    .any { cfgName ->
                        runCatching {
                            configurations.getByName(cfgName).allDependencies.any { it.name == "cmp-observe" }
                        }.getOrDefault(false)
                    }
            },
        )
    }

// Wire generated file into commonMain so compileKotlinCommon picks it up.
// KotlinMultiplatformExtension is NOT on the buildscript compilation classpath of an applied
// script — only on the plugin classpath resolved at runtime. We use reflection so this script
// compiles with only Gradle-core types (always available), and defers the KGP type lookup to
// the configuration phase when the plugin is already applied.
afterEvaluate {
    val ext = project.extensions.findByName("kotlin") ?: return@afterEvaluate
    try {
        @Suppress("UNCHECKED_CAST")
        val sourceSets =
            ext.javaClass.getMethod("getSourceSets").invoke(ext)
                as? org.gradle.api.NamedDomainObjectContainer<Any>
                ?: return@afterEvaluate
        val commonMain = sourceSets.findByName("commonMain") ?: return@afterEvaluate
        val kotlinSrcSet =
            commonMain.javaClass.getMethod("getKotlin").invoke(commonMain)
                as? org.gradle.api.file.SourceDirectorySet
                ?: return@afterEvaluate
        // Wire srcDir to the TaskProvider (not the static path) — Gradle then
        // auto-derives the task dependency for EVERY consumer of this source
        // set (compileKotlinX, androidSourcesJar, X64SourcesJar, etc.). Avoids
        // the brittle task-name-pattern approach that previously missed
        // *SourcesJar publish-time tasks and broke the publish workflow.
        kotlinSrcSet.srcDir(genTask.map { it.outputDir })
    } catch (e: Exception) {
        logger.warn("cmp-observe-metadata: failed to register generated sources in commonMain: ${e.message}")
    }
}

// Belt-and-suspenders: also wire an explicit dependsOn on every consumer task
// pattern we know about. The srcDir(taskProvider) registration above should be
// sufficient on its own, but this guard catches any task that reads the source
// dir via a path other than the kotlinSrcSet API.
//
// Pattern coverage:
//   - compileKotlin{TargetName}            (KMP standard targets)
//   - compileCommonMainKotlinMetadata      (KMP metadata)
//   - compileAndroidMain                   (Android KMP Library plugin)
//   - compile{Variant}KotlinAndroid        (legacy android plugin paths)
//   - {target}SourcesJar / androidSourcesJar / sourcesJar  (publish-time)
tasks
    .matching {
        it.name.startsWith("compileKotlin") ||
            it.name.startsWith("compileCommonMainKotlinMetadata") ||
            it.name.startsWith("compileAndroid") ||
            it.name.endsWith("SourcesJar") ||
            it.name == "sourcesJar"
    }.configureEach { dependsOn(genTask) }
