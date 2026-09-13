/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import com.mobilebytelabs.kmptoolkit.openurl.UrlLauncher
import com.mobilebytelabs.kmptoolkit.openurl.UrlLauncherImpl
import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

/**
 * Ask the user to review the app.
 *
 * Inject this rather than calling the top-level functions, so a test can substitute
 * [com.mobilebytelabs.kmptoolkit.appreview.testing.FakeAppReviewManager] and assert what was
 * requested without a store, a device, or an OS quota in the way.
 *
 * ```kotlin
 * class SettingsViewModel(private val review: AppReviewManager) {
 *     fun onRateTapped() = viewModelScope.launch { review.requestReview() }
 * }
 * ```
 */
public interface AppReviewManager {

    /** What this target can actually do — see [AppReviewCapabilities]. */
    public val capabilities: AppReviewCapabilities

    /**
     * Request a review by the best route this platform has: the native in-app flow where one
     * exists, otherwise the configured store listing.
     *
     * Never throws. Every failure path is a value — see [AppReviewResult].
     */
    public suspend fun requestReview(): AppReviewResult

    /**
     * Open the store listing directly, skipping the native flow even where one exists.
     *
     * This is the honest answer to "the rate button did nothing": the OS quota can swallow a native
     * prompt with no signal, so a visible `Rate on the App Store` action that always navigates is
     * often the better UX. It is also the only route that can be verified in a UI test.
     */
    public fun openStoreListing(): AppReviewResult
}

/**
 * Default [AppReviewManager].
 *
 * Construct it with no arguments — `AppReviewManagerImpl()` — and it reads the listing configured
 * once via [AppReview.configure]. That is what lets it be bound as a plain
 * `single<AppReviewManager> { AppReviewManagerImpl() }` alongside the other platform managers.
 *
 * @param listingOverride pins a listing for this instance instead of the globally configured one.
 *   Leave null in production; useful in a test, or for a host juggling several store identities.
 * @param urlLauncher how the store listing is opened. Defaults to cmp-open-url's launcher, which is
 *   why this module depends on it; injectable so a test need not open anything.
 */
public class AppReviewManagerImpl(
    private val listingOverride: StoreListing? = null,
    private val urlLauncher: UrlLauncher = UrlLauncherImpl(),
) : AppReviewManager {

    // Resolved per call, not captured at construction: a DI graph is typically built before startup
    // configuration runs, so a constructor-captured listing would be empty and nothing would say so.
    private val listing: StoreListing get() = listingOverride ?: AppReview.storeListing

    override val capabilities: AppReviewCapabilities get() = platformAppReviewCapabilities

    override suspend fun requestReview(): AppReviewResult {
        if (capabilities.nativeInAppReview) {
            val native = requestNativeReview()
            // A native failure falls through to the store rather than surfacing an error: the user
            // asked to leave a review, and a reachable listing still satisfies that intent.
            if (native !is AppReviewResult.Failed) {
                report("review_requested", native, native = true)
                return native
            }
        }
        return openStoreListing()
    }

    override fun openStoreListing(): AppReviewResult {
        val url = resolveStoreUrl(listing)
        val result = when {
            url == null -> AppReviewResult.NoStoreConfigured
            urlLauncher.open(url) -> AppReviewResult.StoreOpened(url)
            else -> AppReviewResult.Failed("the platform declined to open $url")
        }
        report("store_listing_opened", result, native = false)
        return result
    }

    /**
     * Reports the OUTCOME and the route, never the store URL.
     *
     * The outcome is the whole diagnostic question here — a native prompt reports nothing about
     * whether it rendered, so "how often did we fall through to the store, and did that work?" is the
     * only observable signal. The URL adds nothing an observer can act on and would put an app
     * identifier into a third-party analytics pipeline.
     */
    private fun report(event: String, result: AppReviewResult, native: Boolean) {
        observeLifecycle(
            cmpMetadata(),
            event,
            mapOf("outcome" to result::class.simpleName, "route" to if (native) "native" else "store"),
        )
    }
}

/**
 * Hand the platform's in-app review request to the OS.
 *
 * Implemented only where such an API exists; every other target returns
 * [AppReviewResult.Failed] so [AppReviewManagerImpl] falls through to the store listing.
 */
internal expect suspend fun requestNativeReview(): AppReviewResult
