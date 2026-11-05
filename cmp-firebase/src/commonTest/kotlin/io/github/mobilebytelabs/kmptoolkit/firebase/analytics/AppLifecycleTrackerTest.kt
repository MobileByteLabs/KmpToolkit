package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Firebase Analytics silently DROPS events whose name it reserves ("Name is reserved"), so a lifecycle event
 * named after one never reaches GA4. `app_background` was one of them.
 */
class AppLifecycleTrackerTest {

    private class RecordingAnalytics : AnalyticsHelper {
        val events = mutableListOf<AnalyticsEvent>()
        override fun logEvent(event: AnalyticsEvent) {
            events += event
        }
    }

    // Firebase's reserved event names (https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event).
    private val reserved = setOf(
        "ad_activeview", "ad_click", "ad_exposure", "ad_query", "ad_reward", "adunit_exposure",
        "app_background", "app_clear_data", "app_exception", "app_remove", "app_store_refund",
        "app_store_subscription_cancel", "app_store_subscription_convert", "app_store_subscription_renew",
        "app_update", "app_upgrade", "dynamic_link_app_open", "dynamic_link_app_update",
        "dynamic_link_first_open", "error", "firebase_campaign", "first_open", "first_visit",
        "in_app_purchase", "notification_dismiss", "notification_foreground", "notification_open",
        "notification_receive", "os_update", "session_start", "session_start_with_rollout", "user_engagement",
    )

    @Test
    fun backgroundAndForeground_areLoggedUnderNamesFirebaseAccepts() {
        val analytics = RecordingAnalytics()
        val tracker = AppLifecycleTracker(analytics)

        tracker.onEnterBackground()
        tracker.onEnterForeground()

        assertEquals(listOf(EventTypes.APP_BACKGROUND, EventTypes.APP_FOREGROUND), analytics.events.map { it.type })
        analytics.events.forEach { assertTrue(it.type !in reserved, "'${it.type}' is reserved by Firebase") }
    }

    @Test
    fun lifecycleEventTypes_avoidFirebaseReservedNames() {
        listOf(EventTypes.APP_LAUNCH, EventTypes.APP_BACKGROUND, EventTypes.APP_FOREGROUND, EventTypes.APP_CRASH)
            .forEach { assertTrue(it !in reserved, "'$it' is reserved by Firebase") }
    }
}
