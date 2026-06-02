package io.github.mobilebytelabs.kmptoolkit.convention

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

internal fun Project.configureDokka() {
    extensions.configure(DokkaExtension::class.java) {
        moduleName.set(project.name)
        moduleVersion.set(providers.gradleProperty("kmptoolkit.version").orElse(""))
        dokkaSourceSets.configureEach {
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            perPackageOption {
                // Suppress internal packages from generated docs
                matchingRegex.set(".*\\.internal.*")
                suppress.set(true)
            }
        }
    }
}
