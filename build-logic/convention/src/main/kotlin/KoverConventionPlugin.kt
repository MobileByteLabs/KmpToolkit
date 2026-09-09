import io.github.mobilebytelabs.kmptoolkit.convention.configureKoverRootReports
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Self-registering kover convention plugin — multi-module aggregation, not per-module reports.
 *
 * - **Root** (applied from the root `plugins {}` block): applies kover and delegates the report
 *   filter + verify rules to `configureKoverRootReports()`, the same way DokkaConventionPlugin
 *   delegates to `configureDokka()`.
 *
 * - **Any leaf module**: applies kover AND self-registers into root's aggregation via
 *   `rootProject.dependencies.add("kover", project(path))`. Each module opts itself in; no
 *   central `subprojects` filter to keep in sync, so adding a new `cmp-*` module needs no change
 *   here — it just applies this plugin.
 *
 * Modelled on mifos-x/kmp-project-template's KoverConventionPlugin.
 */
class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            if (project == rootProject) {
                configureKoverRootReports()
            } else {
                // Root's `kover` configuration exists by now: this plugin applies to root
                // during root build.gradle.kts evaluation, before any subproject configures.
                // project-PATH notation, not the Project object: passing a Project as a
                // dependency notation is deprecated and fails in Gradle 10.
                rootProject.dependencies.add(
                    "kover",
                    rootProject.dependencies.project(mapOf("path" to project.path)),
                )
            }
        }
    }
}
