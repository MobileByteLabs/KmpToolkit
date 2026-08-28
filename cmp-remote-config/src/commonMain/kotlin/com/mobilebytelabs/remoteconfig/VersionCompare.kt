package com.mobilebytelabs.remoteconfig

/**
 * Dotted-number version comparison shared by [RemoteConfigEvaluator]'s min/max app-version gate.
 *
 * Handles both CalVer ("2026.8.4") and SemVer ("1.2.3"), and tolerates a pre-release / build
 * suffix ("2026.6.0-beta.1" → compared as 2026.6.0, suffix ignored) so a gate never mis-fires on
 * a build-flavor suffix. Each dotted segment is reduced to its leading digits, so "8" and "8rc"
 * compare equal on that segment. Non-numeric / blank segments become 0, so a malformed version
 * can never accidentally gate a user in or out — it simply compares as the nearest numeric value.
 */
internal object VersionCompare {

    /** @return negative if [a] < [b], 0 if equal, positive if [a] > [b]. */
    fun compare(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return if (x < y) -1 else 1
        }
        return 0
    }

    private fun parts(v: String): List<Int> =
        v.trim()
            .substringBefore('-') // drop pre-release suffix (…-beta.1)
            .substringBefore('+') // drop build metadata (…+meta)
            .split('.')
            .map { seg -> seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
