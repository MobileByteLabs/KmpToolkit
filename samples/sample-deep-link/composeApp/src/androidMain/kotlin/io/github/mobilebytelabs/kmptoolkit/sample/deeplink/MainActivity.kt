package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Deep links (launch + while-running) are handled automatically by cmp-deep-link's
        // ContentProvider auto-init. No handleDeepLinkIntent() or onNewIntent() override needed.
        setContent { App() }
    }
}
