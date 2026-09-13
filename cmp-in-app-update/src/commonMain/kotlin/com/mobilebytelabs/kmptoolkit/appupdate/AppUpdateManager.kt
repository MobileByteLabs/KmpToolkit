/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appupdate

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

/**
 * What happened when you asked about an update — a flat answer a `when` can exhaust.
 *
 * ## Why this exists alongside [UpdateResult]
 * [UpdateResult.Success] means *the check succeeded*, not *an update is available*: callers still
 * have to reach into `updateInfo.isAvailable` to find out, and every consumer ends up writing the
 * same `UpdateResult -> outcome` mapping. The reference consumer of this library wrote exactly
 * that mapping by hand, as a private `toOutcome()`. That function belongs here.
 *
 * [UpdateResult] remains the low-level result of the [AppUpdate] engine and is unchanged.
 */
public sealed class UpdateOutcome {

    /** Already on the newest version. */
    public data object UpToDate : UpdateOutcome()

    /** An update exists and has NOT been started — you asked to check only. */
    public data class Available(public val info: UpdateInfo) : UpdateOutcome()

    /** An update exists and the platform flow has been started. */
    public data object UpdateStarted : UpdateOutcome()

    /** The user dismissed the update prompt. */
    public data object Cancelled : UpdateOutcome()

    /** This platform cannot do in-app updates. [reason] says why, in words worth showing a dev. */
    public data class NotSupported(public val reason: String) : UpdateOutcome()

    /** Something went wrong — a network failure, a malformed manifest. */
    public data class Failed(public val message: String) : UpdateOutcome()
}

/**
 * Flatten an engine [UpdateResult] into an [UpdateOutcome].
 *
 * [started] distinguishes the two readings of `Success`: after a `checkForUpdate` an available
 * update is [UpdateOutcome.Available]; after a `startUpdate` the same value means
 * [UpdateOutcome.UpdateStarted].
 */
public fun UpdateResult.toOutcome(started: Boolean = false): UpdateOutcome = when (this) {
    is UpdateResult.Success -> when {
        !updateInfo.isAvailable -> UpdateOutcome.UpToDate
        started -> UpdateOutcome.UpdateStarted
        else -> UpdateOutcome.Available(updateInfo)
    }

    is UpdateResult.Cancelled -> UpdateOutcome.Cancelled

    is UpdateResult.NotSupported -> UpdateOutcome.NotSupported(reason)

    is UpdateResult.Error -> UpdateOutcome.Failed(message)
}

/**
 * Injectable app-update entry point.
 *
 * ## Why an interface when [AppUpdate] already exists
 * [AppUpdate] is an `expect object`, so code calling it cannot be faked in tests — and testing an
 * update prompt against a real store is not an option. [AppUpdateManager] is the same capability
 * behind an injectable type, returning [UpdateOutcome] so no caller writes the flattening again.
 *
 * ## Using
 * ```kotlin
 * class UpdateGate(private val updates: AppUpdateManager) {
 *     suspend fun onLaunch() {
 *         when (val outcome = updates.checkAndStart()) {
 *             is UpdateOutcome.UpdateStarted -> Unit          // the OS has it now
 *             is UpdateOutcome.NotSupported -> offerStoreLink()
 *             is UpdateOutcome.Failed -> log(outcome.message)
 *             else -> Unit
 *         }
 *     }
 * }
 * ```
 */
public interface AppUpdateManager {

    /** Whether this platform has a real in-app update flow. */
    public fun isSupported(): Boolean

    /** The running app's version. */
    public fun currentVersion(): AppVersion

    /** Ask whether an update exists, WITHOUT starting anything. */
    public suspend fun check(): UpdateOutcome

    /**
     * Check and, if an update is available, start it in one call.
     *
     * This is the operation the reference consumer hand-wrote: a check that reports availability
     * and never offers the update is the failure worth designing out.
     */
    public suspend fun checkAndStart(updateType: UpdateType = UpdateType.IMMEDIATE): UpdateOutcome

    /** Start the update flow directly, having already decided one is wanted. */
    public suspend fun start(updateType: UpdateType = UpdateType.IMMEDIATE): UpdateOutcome

    /**
     * Send the user to this app's store listing.
     *
     * The fallback when [isSupported] is false — every target that cannot update in-app can still
     * open a store page or a download URL, provided the matching id/URL is set in the config.
     */
    public fun openStore(): Boolean
}

/**
 * The one [AppUpdateManager] — for every target.
 *
 * @param config passed to every [AppUpdate] call.
 * @param defaultUpdateType used when a caller does not name one.
 */
public class AppUpdateManagerImpl(
    private val config: AppUpdateConfig = AppUpdateConfig.Default,
    private val defaultUpdateType: UpdateType = UpdateType.IMMEDIATE,
) : AppUpdateManager {

    override fun isSupported(): Boolean = AppUpdate.isSupported()

    override fun currentVersion(): AppVersion = AppUpdate.getCurrentVersion()

    override suspend fun check(): UpdateOutcome =
        AppUpdate.checkForUpdate(config).toOutcome().also { report("update_checked", it) }

    override suspend fun checkAndStart(updateType: UpdateType): UpdateOutcome {
        val result = AppUpdate.checkForUpdate(config)
        if (result !is UpdateResult.Success) return result.toOutcome()
        if (!result.updateInfo.isAvailable) return UpdateOutcome.UpToDate
        return AppUpdate.startUpdate(updateType, config).toOutcome(started = true)
            .also { report("update_started", it) }
    }

    override suspend fun start(updateType: UpdateType): UpdateOutcome =
        AppUpdate.startUpdate(updateType, config).toOutcome(started = true)
            .also { report("update_started", it) }

    override fun openStore(): Boolean = AppUpdate.openStoreForUpdate(config).also {
        observeLifecycle(cmpMetadata(), "store_opened", mapOf("handled" to it))
    }

    /**
     * Reports the outcome CLASS only.
     *
     * Deliberately not the version strings: an unreleased version name reaching an analytics
     * pipeline is a product leak, and the outcome class is what diagnosis actually needs.
     */
    private fun report(event: String, outcome: UpdateOutcome) {
        observeLifecycle(cmpMetadata(), event, mapOf("outcome" to outcome::class.simpleName))
    }
}
