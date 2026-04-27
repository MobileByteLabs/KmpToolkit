package com.mobilebytelabs.kmptoolkit.deeplink

import android.content.Intent
import androidx.activity.ComponentActivity

/**
 * Convenience extension to handle a deep link [Intent] on a [ComponentActivity].
 *
 * Reads [intent.data] (set by `ACTION_VIEW`) and forwards the URI string to
 * [DeepLinkHandler.handle]. Safe to call from `onCreate` and `onNewIntent`.
 *
 * ## Setup (manual — not required when using auto-init via ContentProvider)
 *
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         handleDeepLinkIntent()           // handles launch intent
 *     }
 *
 *     override fun onNewIntent(intent: Intent) {
 *         super.onNewIntent(intent)
 *         handleDeepLinkIntent(intent)     // handles while-running intent
 *     }
 * }
 * ```
 *
 * @param intent The intent to inspect; defaults to [ComponentActivity.getIntent].
 */
fun ComponentActivity.handleDeepLinkIntent(intent: Intent? = this.intent) {
    intent?.data?.toString()?.let { DeepLinkHandler.handle(it) }
}
