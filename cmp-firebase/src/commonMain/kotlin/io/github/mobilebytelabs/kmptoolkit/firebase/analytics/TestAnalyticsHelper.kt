/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

/**
 * Test implementation of [AnalyticsHelper] that captures events for assertion in tests.
 *
 * Thread-safety: NOT thread-safe — wrap with synchronization if you log events from
 * multiple coroutines concurrently. For most ViewModel tests this isn't an issue.
 *
 * @sample
 * ```kotlin
 * @Test fun `clicking save logs button_click event`() = runTest {
 *     val analytics = TestAnalyticsHelper()
 *     val viewModel = SettingsViewModel(analytics)
 *
 *     viewModel.onSaveClick()
 *
 *     val event = analytics.events.single()
 *     assertEquals(EventTypes.BUTTON_CLICK, event.type)
 *     assertEquals("save", event.extras.first { it.key == ParamKeys.BUTTON_NAME }.value)
 * }
 * ```
 */
class TestAnalyticsHelper : AnalyticsHelper {
    private val _events = mutableListOf<AnalyticsEvent>()
    private val _userProperties = mutableMapOf<String, String>()
    private var _userId: String? = null
    private var _collectionEnabled: Boolean = true
    private var _consent: Pair<Boolean, Boolean>? = null

    /** All captured events, in insertion order. */
    val events: List<AnalyticsEvent> get() = _events.toList()

    /** Captured user properties. */
    val userProperties: Map<String, String> get() = _userProperties.toMap()

    /** Most recent userId (or null if never set). */
    val userId: String? get() = _userId

    /** Current collection-enabled state (defaults to `true`). */
    val collectionEnabled: Boolean get() = _collectionEnabled

    /** Most recent consent as `(analyticsStorage, adStorage)`, or null if never set. */
    val consent: Pair<Boolean, Boolean>? get() = _consent

    override fun logEvent(event: AnalyticsEvent) {
        if (!_collectionEnabled) return // opt-out cascade: mirrors the Firebase/MP tiers
        _events.add(event)
    }

    override fun setUserProperty(name: String, value: String) {
        _userProperties[name] = value
    }

    override fun setUserId(userId: String) {
        _userId = userId
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        _collectionEnabled = enabled
    }

    override fun setConsent(analyticsStorage: Boolean, adStorage: Boolean) {
        _consent = analyticsStorage to adStorage
    }

    /** Clear all captured state — call between tests. */
    fun clear() {
        _events.clear()
        _userProperties.clear()
        _userId = null
        _collectionEnabled = true
        _consent = null
    }

    /** Convenience: count events by type. */
    fun countOf(type: String): Int = _events.count { it.type == type }

    /** Convenience: most recent event of a given type. */
    fun lastOf(type: String): AnalyticsEvent? = _events.lastOrNull { it.type == type }
}
