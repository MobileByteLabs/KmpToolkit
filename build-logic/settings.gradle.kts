// Composite build for kmp-toolkit convention plugins.
// Mirrors the worker-kmp build-logic/ pattern; consumed by the root build via
// `pluginManagement { includeBuild("build-logic") }` in the project settings.gradle.kts.

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
