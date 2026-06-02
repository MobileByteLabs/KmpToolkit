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
        targetExclude("**/build/**", "**/.gradle/**")
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
