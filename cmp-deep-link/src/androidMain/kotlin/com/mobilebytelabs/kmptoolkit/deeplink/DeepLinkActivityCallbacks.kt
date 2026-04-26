package com.mobilebytelabs.kmptoolkit.deeplink

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

/**
 * Application-level activity lifecycle callbacks that auto-register
 * [DeepLinkLifecycleObserver] on every [androidx.activity.ComponentActivity].
 *
 * Installed by [DeepLinkInitProvider] — no consumer setup required.
 */
internal class DeepLinkActivityCallbacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is androidx.activity.ComponentActivity) {
            activity.lifecycle.addObserver(DeepLinkLifecycleObserver(activity))
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
