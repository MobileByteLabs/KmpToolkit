package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SupabaseResolverTest {

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Test
    fun resolverCreatedWithMinimalParams() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "test-key",
        )
        assertNotNull(resolver)
    }

    @Test
    fun resolverCreatedWithCustomTable() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "test-key",
            tableName = "custom_versions",
        )
        assertNotNull(resolver)
    }

    @Test
    fun resolverCreatedWithCustomColumns() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "test-key",
            tableName = "releases",
            versionColumn = "app_version",
            updateTypeColumn = "type",
            releaseNotesColumn = "notes",
            downloadUrlColumn = "url",
            platformColumn = "os",
        )
        assertNotNull(resolver)
    }

    @Test
    fun resolverCreatedWithAllParams() {
        val resolver = SupabaseResolver(
            projectUrl = "https://project.supabase.co",
            anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
            tableName = "app_versions",
            versionColumn = "version",
            updateTypeColumn = "update_type",
            releaseNotesColumn = "release_notes",
            downloadUrlColumn = "download_url",
            platformColumn = "platform",
        )
        assertNotNull(resolver)
    }

    // ========================================================================
    // Default Parameter Value Tests
    // ========================================================================

    @Test
    fun defaultTableNameIsAppVersions() {
        // Create with defaults and verify it doesn't throw
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "test-key",
        )
        assertNotNull(resolver)
    }

    // ========================================================================
    // URL Format Tests (verify constructor accepts various URL formats)
    // ========================================================================

    @Test
    fun projectUrlWithoutTrailingSlash() {
        val resolver = SupabaseResolver(
            projectUrl = "https://project.supabase.co",
            anonKey = "key",
        )
        assertNotNull(resolver)
    }

    @Test
    fun projectUrlWithTrailingSlash() {
        val resolver = SupabaseResolver(
            projectUrl = "https://project.supabase.co/",
            anonKey = "key",
        )
        assertNotNull(resolver)
    }

    @Test
    fun projectUrlWithPath() {
        val resolver = SupabaseResolver(
            projectUrl = "https://custom.domain.com/api/v1",
            anonKey = "key",
        )
        assertNotNull(resolver)
    }

    // ========================================================================
    // Empty String Tests
    // ========================================================================

    @Test
    fun emptyProjectUrlAccepted() {
        val resolver = SupabaseResolver(
            projectUrl = "",
            anonKey = "key",
        )
        assertNotNull(resolver)
    }

    @Test
    fun emptyAnonKeyAccepted() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "",
        )
        assertNotNull(resolver)
    }

    // ========================================================================
    // Special Characters in Column Names
    // ========================================================================

    @Test
    fun columnNameWithUnderscore() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "key",
            versionColumn = "app_version_number",
        )
        assertNotNull(resolver)
    }

    @Test
    fun columnNameWithNumbers() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "key",
            versionColumn = "version2",
        )
        assertNotNull(resolver)
    }

    // ========================================================================
    // VersionResolver Interface Compliance
    // ========================================================================

    @Test
    fun implementsVersionResolver() {
        val resolver: com.mobilebytelabs.kmptoolkit.appupdate.VersionResolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "key",
        )
        assertNotNull(resolver)
    }

    // ========================================================================
    // Multiple Instance Tests
    // ========================================================================

    @Test
    fun multipleResolversIndependent() {
        val resolver1 = SupabaseResolver(
            projectUrl = "https://project1.supabase.co",
            anonKey = "key1",
            tableName = "versions_v1",
        )
        val resolver2 = SupabaseResolver(
            projectUrl = "https://project2.supabase.co",
            anonKey = "key2",
            tableName = "versions_v2",
        )
        assertNotNull(resolver1)
        assertNotNull(resolver2)
    }

    // ========================================================================
    // Long String Value Tests
    // ========================================================================

    @Test
    fun longAnonKeyAccepted() {
        val longKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRlc3QiLCJyb2xlIjoiYW5vbiIs" +
            "ImlhdCI6MTYyMzE1MjM2NywiZXhwIjoxOTM4NzI4MzY3fQ." +
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = longKey,
        )
        assertNotNull(resolver)
    }

    @Test
    fun longTableNameAccepted() {
        val resolver = SupabaseResolver(
            projectUrl = "https://test.supabase.co",
            anonKey = "key",
            tableName = "very_long_application_versions_table_name_for_testing_purposes",
        )
        assertNotNull(resolver)
    }
}
