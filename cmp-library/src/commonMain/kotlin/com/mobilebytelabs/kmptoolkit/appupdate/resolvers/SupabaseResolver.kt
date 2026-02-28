package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.appupdate.VersionInfo
import com.mobilebytelabs.kmptoolkit.appupdate.VersionResolver

/**
 * Version resolver using Supabase REST API.
 *
 * Queries a Supabase table for the latest version information.
 * No Supabase SDK required - uses REST API directly.
 *
 * ## Usage
 *
 * ```kotlin
 * // Via builder (recommended)
 * val config = AppUpdateConfig.builder()
 *     .android("com.example.app")
 *     .ios("123456789")
 *     .supabase(
 *         projectUrl = "https://xxx.supabase.co",
 *         anonKey = "eyJ...",
 *     )
 *     .build()
 *
 * // Direct usage with custom columns
 * val resolver = SupabaseResolver(
 *     projectUrl = "https://xxx.supabase.co",
 *     anonKey = "eyJ...",
 *     tableName = "releases",
 *     versionColumn = "app_version",
 * )
 * val versionInfo = resolver.resolve()
 * ```
 *
 * ## Required Table Schema
 *
 * ```sql
 * CREATE TABLE app_versions (
 *     id SERIAL PRIMARY KEY,
 *     version TEXT NOT NULL,
 *     update_type TEXT DEFAULT 'FLEXIBLE',  -- 'FLEXIBLE' or 'IMMEDIATE'
 *     release_notes TEXT,
 *     download_url TEXT,
 *     platform TEXT DEFAULT 'all',  -- 'all', 'windows', 'macos', 'linux'
 *     created_at TIMESTAMPTZ DEFAULT NOW()
 * );
 *
 * -- Enable Row Level Security
 * ALTER TABLE app_versions ENABLE ROW LEVEL SECURITY;
 *
 * -- Allow public read access
 * CREATE POLICY "Public read" ON app_versions
 *     FOR SELECT USING (true);
 *
 * -- Create index for fast lookup
 * CREATE INDEX idx_app_versions_platform ON app_versions(platform, created_at DESC);
 * ```
 *
 * ## Inserting a New Version
 *
 * ```sql
 * INSERT INTO app_versions (version, update_type, release_notes, download_url)
 * VALUES ('1.2.3', 'FLEXIBLE', 'Bug fixes and improvements', 'https://...');
 * ```
 *
 * @property projectUrl Supabase project URL (e.g., "https://xxx.supabase.co")
 * @property anonKey Supabase anon/public key for authentication
 * @property tableName Table name (default: "app_versions")
 * @property versionColumn Column name for version (default: "version")
 * @property updateTypeColumn Column name for update type (default: "update_type")
 * @property releaseNotesColumn Column name for release notes (default: "release_notes")
 * @property downloadUrlColumn Column name for download URL (default: "download_url")
 * @property platformColumn Column name for platform filter (default: "platform")
 * @since 0.5.0
 */
class SupabaseResolver(
    private val projectUrl: String,
    private val anonKey: String,
    private val tableName: String = "app_versions",
    private val versionColumn: String = "version",
    private val updateTypeColumn: String = "update_type",
    private val releaseNotesColumn: String = "release_notes",
    private val downloadUrlColumn: String = "download_url",
    private val platformColumn: String = "platform",
) : VersionResolver {

    /**
     * Resolves the latest version from Supabase.
     *
     * @return [VersionInfo] with version details from the database
     * @throws Exception if network request fails or no version found
     */
    override suspend fun resolve(): VersionInfo {
        val url = buildQueryUrl()
        val headers = buildHeaders()
        val response = HttpClient.get(url, headers)
        return parseSupabaseResponse(response)
    }

    private fun buildQueryUrl(): String {
        val platform = PlatformDetector.detect()
        val baseUrl = "${projectUrl.removeSuffix("/")}/rest/v1/$tableName"

        // Select only needed columns
        val select = "select=$versionColumn,$updateTypeColumn,$releaseNotesColumn,$downloadUrlColumn"

        // Filter: platform = 'all' OR platform = current_platform
        val filter = "or=($platformColumn.eq.all,$platformColumn.eq.$platform)"

        // Order by created_at descending, limit 1
        val order = "order=created_at.desc"
        val limit = "limit=1"

        return "$baseUrl?$select&$filter&$order&$limit"
    }

    private fun buildHeaders(): Map<String, String> = mapOf(
        "apikey" to anonKey,
        "Authorization" to "Bearer $anonKey",
        "Accept" to "application/json",
    )

    private fun parseSupabaseResponse(response: String): VersionInfo {
        // Response is a JSON array, extract first element
        val trimmed = response.trim()
        if (!trimmed.startsWith("[") || trimmed == "[]") {
            throw IllegalStateException("No version found in Supabase table '$tableName'")
        }

        // Extract first object from array
        val start = trimmed.indexOf('{')
        val end = trimmed.indexOf('}')
        if (start == -1 || end == -1) {
            throw IllegalStateException("Invalid response format from Supabase")
        }
        val json = trimmed.substring(start, end + 1)

        // Parse fields
        val version = extractJsonString(json, versionColumn)
            ?: throw IllegalStateException("No $versionColumn found in response")

        val updateTypeStr = extractJsonString(json, updateTypeColumn)
        val updateType = when (updateTypeStr?.uppercase()) {
            "IMMEDIATE" -> UpdateType.IMMEDIATE
            "FLEXIBLE" -> UpdateType.FLEXIBLE
            "NONE" -> UpdateType.NONE
            else -> UpdateType.FLEXIBLE
        }

        val releaseNotes = extractJsonString(json, releaseNotesColumn)
        val downloadUrl = extractJsonString(json, downloadUrlColumn)

        return VersionInfo(
            version = version,
            updateType = updateType,
            releaseNotes = releaseNotes,
            downloadUrl = downloadUrl,
            metadata = mapOf(
                "source" to "supabase",
                "table" to tableName,
            ),
        )
    }

    private fun extractJsonString(json: String, key: String): String? {
        // Handle both quoted strings and null values
        val pattern = """"$key"\s*:\s*(?:"([^"\\]*(?:\\.[^"\\]*)*)"|null)""".toRegex()
        val match = pattern.find(json) ?: return null
        val value = match.groupValues[1]
        if (value.isEmpty() && match.value.contains("null")) return null
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
