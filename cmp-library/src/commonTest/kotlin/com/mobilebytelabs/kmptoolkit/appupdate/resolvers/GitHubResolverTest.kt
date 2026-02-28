package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubResolverTest {

    // ========================================================================
    // Asset Pattern Tests
    // ========================================================================

    @Test
    fun windowsPatternMatchesExe() {
        val patterns = GitHubResolver.ASSET_PATTERNS["windows"]!!
        val matches = patterns.any { pattern ->
            "app-windows.exe".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun windowsPatternMatchesMsi() {
        val patterns = GitHubResolver.ASSET_PATTERNS["windows"]!!
        val matches = patterns.any { pattern ->
            "installer.msi".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun windowsPatternMatchesWin64() {
        val patterns = GitHubResolver.ASSET_PATTERNS["windows"]!!
        val matches = patterns.any { pattern ->
            "app-win64.exe".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun macosPatternMatchesDmg() {
        val patterns = GitHubResolver.ASSET_PATTERNS["macos"]!!
        val matches = patterns.any { pattern ->
            "app-macos.dmg".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun macosPatternMatchesPkg() {
        val patterns = GitHubResolver.ASSET_PATTERNS["macos"]!!
        val matches = patterns.any { pattern ->
            "installer.pkg".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun macosPatternMatchesDarwin() {
        val patterns = GitHubResolver.ASSET_PATTERNS["macos"]!!
        val matches = patterns.any { pattern ->
            "app-darwin-arm64.dmg".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun linuxPatternMatchesAppImage() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "app-linux.AppImage".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun linuxPatternMatchesDeb() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "app-1.0.0.deb".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun linuxPatternMatchesRpm() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "app-1.0.0.rpm".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun linuxPatternMatchesTarGz() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "app-linux-x64.tar.gz".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    // ========================================================================
    // Pattern Non-Match Tests
    // ========================================================================

    @Test
    fun windowsPatternDoesNotMatchLinux() {
        val patterns = GitHubResolver.ASSET_PATTERNS["windows"]!!
        val matches = patterns.any { pattern ->
            "app-linux.AppImage".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertFalse(matches)
    }

    @Test
    fun macosPatternDoesNotMatchWindows() {
        val patterns = GitHubResolver.ASSET_PATTERNS["macos"]!!
        val matches = patterns.any { pattern ->
            "app-windows.exe".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertFalse(matches)
    }

    @Test
    fun linuxPatternDoesNotMatchMacos() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "app-macos.dmg".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertFalse(matches)
    }

    // ========================================================================
    // Asset Patterns Structure Tests
    // ========================================================================

    @Test
    fun assetPatternsContainsWindows() {
        assertTrue(GitHubResolver.ASSET_PATTERNS.containsKey("windows"))
    }

    @Test
    fun assetPatternsContainsMacos() {
        assertTrue(GitHubResolver.ASSET_PATTERNS.containsKey("macos"))
    }

    @Test
    fun assetPatternsContainsLinux() {
        assertTrue(GitHubResolver.ASSET_PATTERNS.containsKey("linux"))
    }

    @Test
    fun windowsPatternsNotEmpty() {
        assertTrue(GitHubResolver.ASSET_PATTERNS["windows"]!!.isNotEmpty())
    }

    @Test
    fun macosPatternsNotEmpty() {
        assertTrue(GitHubResolver.ASSET_PATTERNS["macos"]!!.isNotEmpty())
    }

    @Test
    fun linuxPatternsNotEmpty() {
        assertTrue(GitHubResolver.ASSET_PATTERNS["linux"]!!.isNotEmpty())
    }

    // ========================================================================
    // Case Insensitivity Tests
    // ========================================================================

    @Test
    fun windowsPatternCaseInsensitive() {
        val patterns = GitHubResolver.ASSET_PATTERNS["windows"]!!
        val matches = patterns.any { pattern ->
            "APP-WINDOWS.EXE".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun macosPatternCaseInsensitive() {
        val patterns = GitHubResolver.ASSET_PATTERNS["macos"]!!
        val matches = patterns.any { pattern ->
            "APP-MACOS.DMG".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun linuxPatternCaseInsensitive() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "APP.APPIMAGE".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    // ========================================================================
    // Pattern Count Tests
    // ========================================================================

    @Test
    fun windowsHasMultiplePatterns() {
        assertEquals(5, GitHubResolver.ASSET_PATTERNS["windows"]!!.size)
    }

    @Test
    fun macosHasMultiplePatterns() {
        assertEquals(5, GitHubResolver.ASSET_PATTERNS["macos"]!!.size)
    }

    @Test
    fun linuxHasMultiplePatterns() {
        assertEquals(7, GitHubResolver.ASSET_PATTERNS["linux"]!!.size)
    }

    // ========================================================================
    // Edge Case Pattern Tests
    // ========================================================================

    @Test
    fun windowsPatternMatchesWithVersion() {
        val patterns = GitHubResolver.ASSET_PATTERNS["windows"]!!
        val matches = patterns.any { pattern ->
            "myapp-1.2.3-windows-x64.exe".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun macosPatternMatchesWithArch() {
        val patterns = GitHubResolver.ASSET_PATTERNS["macos"]!!
        val matches = patterns.any { pattern ->
            "myapp-1.0.0-macos-arm64.dmg".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }

    @Test
    fun linuxPatternMatchesStandaloneAppImage() {
        val patterns = GitHubResolver.ASSET_PATTERNS["linux"]!!
        val matches = patterns.any { pattern ->
            "MyApp-x86_64.AppImage".matches(pattern.toRegex(RegexOption.IGNORE_CASE))
        }
        assertTrue(matches)
    }
}
