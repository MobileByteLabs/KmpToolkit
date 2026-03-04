package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.appupdate.VersionInfo
import com.mobilebytelabs.kmptoolkit.appupdate.VersionResolver

/**
 * Version resolver using GitHub Releases API.
 *
 * Fetches the latest release from a GitHub repository and extracts:
 * - Version from `tag_name` (strips leading 'v' if present)
 * - Release notes from `body`
 * - Download URL from `assets` based on platform
 *
 * ## Usage
 *
 * ```kotlin
 * // Via builder (recommended)
 * val config = AppUpdateConfig.builder()
 *     .android("com.example.app")
 *     .ios("123456789")
 *     .github(owner = "user", repo = "my-app")
 *     .build()
 *
 * // Direct usage
 * val resolver = GitHubResolver(
 *     owner = "user",
 *     repo = "my-app",
 *     token = "ghp_xxx",  // Optional: for private repos
 * )
 * val versionInfo = resolver.resolve()
 * ```
 *
 * ## Asset Naming Convention
 *
 * Name your release assets with platform identifiers for automatic detection:
 * - Windows: `app-1.0.0-windows.exe`, `app-1.0.0.msi`, `*-win-*.exe`
 * - macOS: `app-1.0.0-macos.dmg`, `app-1.0.0.pkg`, `*-mac-*.dmg`
 * - Linux: `app-1.0.0-linux.AppImage`, `*.deb`, `*.rpm`
 *
 * ## GitHub API Rate Limits
 *
 * - Unauthenticated: 60 requests/hour
 * - Authenticated (with token): 5000 requests/hour
 *
 * For apps with many users, consider providing a token or
 * implementing caching on your side.
 *
 * @property owner GitHub username or organization
 * @property repo Repository name
 * @property token Optional: Personal access token for private repos or higher rate limits
 * @property includePreRelease Include pre-releases when checking (default: false)
 * @since 0.5.0
 */
class GitHubResolver(
    private val owner: String,
    private val repo: String,
    private val token: String? = null,
    private val includePreRelease: Boolean = false,
) : VersionResolver {

    /**
     * Resolves the latest version from GitHub Releases.
     *
     * @return [VersionInfo] with version, release notes, and download URL
     * @throws Exception if network request fails or response parsing fails
     */
    override suspend fun resolve(): VersionInfo {
        val url = buildApiUrl()
        val response = HttpClient.get(url, buildHeaders())
        return parseGitHubResponse(response)
    }

    private fun buildApiUrl(): String = if (includePreRelease) {
        // Get all releases and pick the first one (includes pre-releases)
        "https://api.github.com/repos/$owner/$repo/releases?per_page=1"
    } else {
        // Get only the latest stable release
        "https://api.github.com/repos/$owner/$repo/releases/latest"
    }

    private fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )
        token?.let { headers["Authorization"] = "Bearer $it" }
        return headers
    }

    private fun parseGitHubResponse(response: String): VersionInfo {
        // Handle array response for pre-release query
        val json = if (response.trimStart().startsWith("[")) {
            // Extract first element from array
            extractFirstFromArray(response)
        } else {
            response
        }

        val tagName = extractJsonString(json, "tag_name")
            ?: throw IllegalStateException("No tag_name found in GitHub response")

        // Strip leading 'v' from version tag
        val version = tagName.removePrefix("v").removePrefix("V")

        val body = extractJsonString(json, "body")
        val htmlUrl = extractJsonString(json, "html_url")

        // Find download URL from assets based on current platform
        val downloadUrl = findPlatformAsset(json)

        return VersionInfo(
            version = version,
            updateType = UpdateType.FLEXIBLE,
            releaseNotes = body,
            downloadUrl = downloadUrl,
            storeUrl = htmlUrl,
            metadata = mapOf(
                "source" to "github",
                "owner" to owner,
                "repo" to repo,
            ),
        )
    }

    private fun extractFirstFromArray(json: String): String {
        // Simple extraction of first object from JSON array
        val start = json.indexOf('{')
        if (start == -1) throw IllegalStateException("Empty releases array")

        var depth = 0
        var end = start
        for (i in start until json.length) {
            when (json[i]) {
                '{' -> depth++

                '}' -> {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
        }
        return json.substring(start, end + 1)
    }

    private fun extractJsonString(json: String, key: String): String? {
        // Simple regex-based JSON string extraction
        val pattern = """"$key"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groupValues[1]
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun findPlatformAsset(json: String): String? {
        val platform = PlatformDetector.detect()
        val patterns = ASSET_PATTERNS[platform] ?: return null

        // Extract assets array and find matching URL
        val assetsStart = json.indexOf("\"assets\"")
        if (assetsStart == -1) return null

        val arrayStart = json.indexOf('[', assetsStart)
        val arrayEnd = findMatchingBracket(json, arrayStart, '[', ']')
        if (arrayStart == -1 || arrayEnd == -1) return null

        val assetsJson = json.substring(arrayStart, arrayEnd + 1)

        // Find all browser_download_url values
        val urlPattern = """"browser_download_url"\s*:\s*"([^"]+)"""".toRegex()
        val urls = urlPattern.findAll(assetsJson).map { it.groupValues[1] }.toList()

        // Find first URL matching any of our patterns
        for (pattern in patterns) {
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            val match = urls.find { regex.matches(it.substringAfterLast('/')) }
            if (match != null) return match
        }

        return null
    }

    private fun findMatchingBracket(json: String, start: Int, open: Char, close: Char): Int {
        if (start == -1 || json[start] != open) return -1
        var depth = 0
        for (i in start until json.length) {
            when (json[i]) {
                open -> depth++

                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    companion object {
        /**
         * Asset name patterns for each platform.
         * First matching pattern wins.
         */
        val ASSET_PATTERNS = mapOf(
            "windows" to listOf(
                ".*windows.*\\.exe$",
                ".*win.*\\.exe$",
                ".*\\.msi$",
                ".*-win64\\.exe$",
                ".*-win\\.exe$",
            ),
            "macos" to listOf(
                ".*macos.*\\.dmg$",
                ".*mac.*\\.dmg$",
                ".*darwin.*\\.dmg$",
                ".*\\.pkg$",
                ".*-mac\\.dmg$",
            ),
            "linux" to listOf(
                ".*linux.*\\.AppImage$",
                ".*\\.AppImage$",
                ".*linux.*\\.deb$",
                ".*\\.deb$",
                ".*linux.*\\.rpm$",
                ".*\\.rpm$",
                ".*linux.*\\.tar\\.gz$",
            ),
        )
    }
}

/**
 * Simple platform detector for determining current OS.
 */
internal expect object PlatformDetector {
    /**
     * Detects the current platform.
     * @return "windows", "macos", "linux", or "unknown"
     */
    fun detect(): String
}

/**
 * Simple HTTP client for making GET requests.
 * Platform-specific implementations handle the actual HTTP calls.
 */
internal expect object HttpClient {
    /**
     * Makes a GET request to the specified URL.
     *
     * @param url The URL to fetch
     * @param headers Optional headers to include
     * @return Response body as string
     * @throws Exception if request fails
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String
}
