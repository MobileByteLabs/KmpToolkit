package com.mobilebytelabs.kmptoolkit.deeplink

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Lifecycle-aware observer that automatically handles deep link intents on
 * `onCreate` and `onNewIntent`.
 *
 * Registered automatically by [DeepLinkActivityCallbacks] (installed via
 * [DeepLinkInitProvider]). You can also register it manually if you prefer
 * explicit control:
 *
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         lifecycle.addObserver(DeepLinkLifecycleObserver(this))
 *     }
 * }
 * ```
 */
class DeepLinkLifecycleObserver(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        activity.handleDeepLinkIntent()
    }

    /** Call from [ComponentActivity.onNewIntent] to handle while-running links. */
    fun onNewIntent(intent: Intent) {
        activity.handleDeepLinkIntent(intent)
    }
}
