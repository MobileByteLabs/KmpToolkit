plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)

    // Kover — ROOT-LEVEL AGGREGATION, not per-module reports.
    //
    // `apply false` is the classpath hook that makes `org.jetbrains.kotlinx.kover` resolvable
    // from KoverConventionPlugin's `pluginManager.apply(id)` call (same pattern as dokka above).
    // The convention plugin below then applies to root, configures the filters + verify rule,
    // and each cmp-* module self-registers into the aggregation by applying the same plugin.
    //
    // A per-module report across 23 modules says nothing about the toolkit's overall coverage;
    // Kover's own multi-module KMP guide recommends root aggregation.
    // Tasks: ./gradlew koverHtmlReport | koverXmlReport | koverVerify
    alias(libs.plugins.kover) apply false
    id("io.github.mobilebytelabs.kmptoolkit.kover")
}

// Dokka is applied per-module via the `io.github.mobilebytelabs.kmptoolkit.dokka`
// convention plugin from `build-logic/convention/` — the `apply false` above
// is the classpath hook that makes `org.jetbrains.dokka` resolvable from the
// convention plugin's `pluginManager.apply("org.jetbrains.dokka")` call.
// Convention pattern mirrors worker-kmp; see
// build-logic/convention/src/main/kotlin/DokkaConventionPlugin.kt.

// Detekt configuration for the entire project
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
    }
    jvmTarget = "11"
}

// Spotless configuration for code formatting
spotless {
    kotlin {
        target("**/*.kt")
        // Samples are demo apps with long UI explanatory strings — exclude from strict library-grade formatting.
        targetExclude("**/build/**/*.kt", "**/.gradle/**/*.kt", "**/samples/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "android" to "true",
                    "indent_size" to "4",
                    "continuation_indent_size" to "4",
                    "max_line_length" to "120",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_package-name" to "disabled",
                ),
            )
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    format("misc") {
        target("**/*.md", "**/.gitignore", "**/*.yaml", "**/*.yml")
        // .swiftpm-locks / .swiftpm hold GitLive 3.x's firebase-ios-sdk SwiftPM checkout
        // (vendored third-party .md files) — never format them.
        targetExclude("**/build/**", "**/.gradle/**", "**/.swiftpm-locks/**", "**/.swiftpm/**")
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}

// Configure the existing check task to include Detekt and Spotless
tasks.named("check") {
    dependsOn("detekt", "spotlessCheck")
}

// Task to apply all fixes
tasks.register("fix") {
    group = "verification"
    description = "Applies all automatic fixes including Spotless formatting"
    dependsOn("spotlessApply")
}

// Swift-interop link path for Apple native binaries (supabase-kt 3.8.0+).
//
// supabase-kt 3.8.0's auth-kt pulls dev.whyoleg.cryptography's "optimal" provider → CryptoKit on Apple,
// whose cinterop klib carries a HARDCODED Swift runtime path under `/Applications/Xcode.app/...`. With
// Xcode installed under any other name (Xcode-26.5.0.app, xcodes, ...) the swiftCompatibility* archives
// are not found and linking the test executables fails with
//   Undefined symbols: __swift_FORCE_LOAD_$_swiftCompatibility56
// Resolve the ACTIVE toolchain from `xcode-select -p` and add its per-SDK lib dir as an explicit -L.
// Same recipe as cmp-paycraft (which hit it first), extended to every Apple target the toolkit builds.
// No-op off macOS or where a directory is absent.
val swiftLibRoot: File? =
    runCatching {
        check(System.getProperty("os.name").startsWith("Mac")) { "Apple toolchain lookup is macOS-only" }
        val developerDir =
            providers
                .exec { commandLine("xcode-select", "-p") }
                .standardOutput.asText
                .get()
                .trim()
        File(developerDir, "Toolchains/XcodeDefault.xctoolchain/usr/lib/swift").takeIf { it.isDirectory }
    }.getOrNull()

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val root = swiftLibRoot ?: return@withId
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java).configureEach {
                val sdk =
                    when (konanTarget.name) {
                        "ios_arm64" -> "iphoneos"
                        "ios_x64", "ios_simulator_arm64" -> "iphonesimulator"
                        "macos_arm64", "macos_x64" -> "macosx"
                        "tvos_arm64" -> "appletvos"
                        "tvos_x64", "tvos_simulator_arm64" -> "appletvsimulator"
                        "watchos_arm32", "watchos_arm64", "watchos_device_arm64" -> "watchos"
                        "watchos_x64", "watchos_simulator_arm64" -> "watchsimulator"
                        else -> null
                    } ?: return@configureEach
                val dir = File(root, sdk).takeIf { it.isDirectory } ?: return@configureEach
                binaries.all { linkerOpts("-L${dir.absolutePath}") }
            }
        }
    }
}
