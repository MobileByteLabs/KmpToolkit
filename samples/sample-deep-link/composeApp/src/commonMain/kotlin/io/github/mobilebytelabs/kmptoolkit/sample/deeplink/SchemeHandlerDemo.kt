package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler

private val SAMPLE_LINKS = listOf(
    "Product detail" to "demo://open/product/42?ref=banner&tab=reviews",
    "User profile" to "demo://open/user/rajan/posts?tab=videos",
    "Search" to "demo://open/search?q=kotlin+multiplatform&lang=en",
    "External HTTPS" to "https://example.com/promo/summer-sale?code=SAVE20#terms",
    "Minimal (no query)" to "demo://open/home",
)

/**
 * Demonstrates receiving a deep link URI and displaying all its parsed components.
 *
 * In production, the OS calls [DeepLinkHandler.handle] via the platform entry point:
 * - **Android**: auto-init ContentProvider fires on launch; `handleDeepLinkIntent()` on `onCreate`/`onNewIntent`
 * - **iOS/macOS**: `DeepLinkAppleHelper.shared.handleUrl(url)` in AppDelegate or `.onOpenURL`
 * - **JVM Desktop**: `DeepLinkHandler.handleLaunchArgs(args)` in `main()`
 * - **Browser**: `DeepLinkHandler.initBrowser()` at app startup
 *
 * ADB test command (Android):
 * ```
 * adb shell am start -a android.intent.action.VIEW \
 *   -d "demo://open/product/42?ref=banner" \
 *   io.github.mobilebytelabs.kmptoolkit.sample.deeplink
 * ```
 */
@Composable
fun SchemeHandlerDemo() {
    val lastLink by DeepLinkHandler.lastReceived.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Scheme Handler", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap a link to simulate OS delivery. The parsed DeepLink is shown below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Sample link buttons
        SAMPLE_LINKS.forEach { (label, uri) ->
            SampleLinkButton(label = label, uri = uri)
        }

        OutlinedButton(
            onClick = { DeepLinkHandler.clear() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear")
        }

        HorizontalDivider()

        // Parsed result
        if (lastLink != null) {
            val link = lastLink!!
            Text("Parsed DeepLink", style = MaterialTheme.typography.labelMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DeepLinkField("raw", link.raw)
                    DeepLinkField("scheme", link.scheme)
                    DeepLinkField("host", link.host)
                    DeepLinkField("path", link.path)
                    DeepLinkField("pathSegments", link.pathSegments.joinToString(", ").ifEmpty { "—" })
                    if (link.queryParams.isNotEmpty()) {
                        link.queryParams.entries.forEachIndexed { i, (k, v) ->
                            DeepLinkField("query[$k]", v, dim = i > 0)
                        }
                    } else {
                        DeepLinkField("queryParams", "—")
                    }
                    DeepLinkField("fragment", link.fragment ?: "—")
                }
            }
        } else {
            Text(
                "No link received yet. Tap a button above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ADB hint
        HorizontalDivider()
        Text("Test from terminal (Android)", style = MaterialTheme.typography.labelMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = "adb shell am start -a android.intent.action.VIEW \\\n" +
                    "  -d \"demo://open/product/42?ref=adb\" \\\n" +
                    "  io.github.mobilebytelabs.kmptoolkit.sample.deeplink",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun SampleLinkButton(label: String, uri: String) {
    Button(
        onClick = { DeepLinkHandler.handle(uri) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                uri,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DeepLinkField(name: String, value: String, dim: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$name:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = if (dim) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
