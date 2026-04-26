package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobilebytelabs.kmptoolkit.deeplink.handleDeepLinkIntent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle deep link from launch intent (auto-init via ContentProvider also fires,
        // but explicit call ensures ordering with compose setup)
        handleDeepLinkIntent()
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }
}
