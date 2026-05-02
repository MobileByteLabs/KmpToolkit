package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Constraints that can be checked against the current [NetworkStatus].
 *
 * Use with [NetworkMonitor.executeWithConstraint] or [NetworkConstraint.isSatisfiedBy]
 * to gate operations on specific network conditions.
 */
sealed class NetworkConstraint {

    /** Requires any active connection. */
    data object RequiresConnectivity : NetworkConstraint()

    /** Requires WiFi connection. */
    data object RequiresWiFi : NetworkConstraint()

    /** Requires an unmetered connection. */
    data object RequiresUnmetered : NetworkConstraint()

    /** Requires minimum downstream bandwidth in kbps. */
    data class RequiresMinBandwidth(val minKbps: Int) : NetworkConstraint()

    /**
     * Check if this constraint is satisfied by the given [NetworkStatus].
     */
    fun isSatisfiedBy(status: NetworkStatus): Boolean = when (this) {
        is RequiresConnectivity -> status is NetworkStatus.Available

        is RequiresWiFi ->
            status is NetworkStatus.Available &&
                status.info.type == NetworkType.WiFi

        is RequiresUnmetered ->
            status is NetworkStatus.Available &&
                !status.info.isMetered

        is RequiresMinBandwidth ->
            status is NetworkStatus.Available &&
                status.info.downstreamBandwidthKbps >= minKbps
    }

    companion object {
        /**
         * Create a composite constraint that requires ALL given constraints to be satisfied.
         */
        fun allOf(vararg constraints: NetworkConstraint): List<NetworkConstraint> = constraints.toList()
    }
}

/**
 * Execute [block] only if all [constraints] are satisfied by current network status.
 *
 * @throws NetworkUnavailableException if any constraint is not met.
 */
suspend fun <T> NetworkMonitor.executeWithConstraint(
    vararg constraints: NetworkConstraint,
    block: suspend () -> T,
): T {
    val status = networkStatus.value
    for (constraint in constraints) {
        if (!constraint.isSatisfiedBy(status)) {
            throw NetworkUnavailableException(
                "Network constraint not met: $constraint (current: $status)",
            )
        }
    }
    return block()
}

/**
 * Check if all [constraints] are currently satisfied.
 */
fun NetworkMonitor.checkConstraints(vararg constraints: NetworkConstraint): Boolean {
    val status = networkStatus.value
    return constraints.all { it.isSatisfiedBy(status) }
}
