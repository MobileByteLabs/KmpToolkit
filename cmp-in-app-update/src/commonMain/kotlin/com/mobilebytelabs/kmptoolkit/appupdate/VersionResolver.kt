package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Interface for resolving version information from any backend.
 *
 * Implement this interface to integrate with your own version check backend.
 * Built-in implementations: [GitHubResolver], [SupabaseResolver]
 *
 * ## Built-in Resolvers
 *
 * ```kotlin
 * // GitHub Releases
 * val config = AppUpdateConfig.builder()
 *     .github(owner = "user", repo = "my-app")
 *     .build()
 *
 * // Supabase
 * val config = AppUpdateConfig.builder()
 *     .supabase(
 *         projectUrl = "https://xxx.supabase.co",
 *         anonKey = "eyJ...",
 *     )
 *     .build()
 * ```
 *
 * ## Custom Implementation
 *
 * ```kotlin
 * class MyBackendResolver(
 *     private val apiUrl: String,
 * ) : VersionResolver {
 *     override suspend fun resolve(): VersionInfo {
 *         // Fetch from your API
 *         val response = fetchVersion(apiUrl)
 *         return VersionInfo(
 *             version = response.version,
 *             updateType = UpdateType.FLEXIBLE,
 *             downloadUrl = response.downloadUrl,
 *             releaseNotes = response.changelog,
 *         )
 *     }
 * }
 *
 * // Usage
 * val config = AppUpdateConfig.builder()
 *     .versionResolver(MyBackendResolver("https://api.myapp.com"))
 *     .build()
 * ```
 *
 * @since 0.5.0
 */
interface VersionResolver {
    /**
     * Resolve the latest version information from the backend.
     *
     * This method is called when checking for updates on platforms that
     * don't have native store APIs (JVM, Linux, Windows).
     *
     * @return [VersionInfo] containing the latest version details
     * @throws Exception if resolution fails (network error, parse error, etc.)
     */
    suspend fun resolve(): VersionInfo
}
