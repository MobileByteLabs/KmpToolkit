package com.mobilebytelabs.remoteconfig

import kotlin.test.Test
import kotlin.test.assertTrue

class VersionCompareTest {

    private fun lt(a: String, b: String) = assertTrue(VersionCompare.compare(a, b) < 0, "$a should be < $b")
    private fun gt(a: String, b: String) = assertTrue(VersionCompare.compare(a, b) > 0, "$a should be > $b")
    private fun eq(a: String, b: String) = assertTrue(VersionCompare.compare(a, b) == 0, "$a should == $b")

    @Test
    fun calVerOrdering() {
        lt("2026.7.5", "2026.8.0")
        lt("2026.7.99", "2026.8.0")
        gt("2026.8.4", "2026.8.3")
        gt("2026.8.0", "2026.7.99")
        eq("2026.8.4", "2026.8.4")
    }

    @Test
    fun semVerOrdering() {
        lt("1.2.3", "1.2.4")
        lt("1.9.0", "1.10.0") // numeric, not lexical
        gt("2.0.0", "1.99.99")
        eq("3.0.0", "3.0.0")
    }

    @Test
    fun differingSegmentCountsPadWithZero() {
        eq("2026.8", "2026.8.0")
        lt("2026.8", "2026.8.1")
        gt("2026.8.1", "2026.8")
    }

    @Test
    fun preReleaseAndBuildSuffixesAreIgnored() {
        eq("2026.6.0-beta.1", "2026.6.0")
        eq("2026.8.4+ci.42", "2026.8.4")
        lt("2026.6.0-beta.1", "2026.8.0")
    }

    @Test
    fun malformedSegmentsDegradeToNearestNumber() {
        // Non-digit leading segment ⇒ 0; trailing letters stripped.
        eq("v2026.8.4", "0.8.4") // "v2026" → leading digits none → 0
        eq("2026.8rc.4", "2026.8.4") // "8rc" → 8
        eq("", "0.0.0")
    }

    @Test
    fun theUpdateGateWindow() {
        // max_app_version = "2026.7.99" ⇒ everything at/below stays inside the window (nagged),
        // and the first 2026.8.x build falls outside it (no longer nagged).
        assertTrue(VersionCompare.compare("2026.6.0", "2026.7.99") <= 0)
        assertTrue(VersionCompare.compare("2026.7.5", "2026.7.99") <= 0)
        assertTrue(VersionCompare.compare("2026.8.0", "2026.7.99") > 0)
        assertTrue(VersionCompare.compare("2026.8.4", "2026.7.99") > 0)
    }
}
