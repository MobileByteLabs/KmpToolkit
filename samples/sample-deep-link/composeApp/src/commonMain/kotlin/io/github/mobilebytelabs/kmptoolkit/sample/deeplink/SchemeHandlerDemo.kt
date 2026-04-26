package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler

/**
 * Demonstrates receiving a deep link URI and displaying its parsed components.
 *
 * In production: the OS calls DeepLinkHandler.handle() via the platform entry point
 * (Android: handleDeepLinkIntent, iOS: DeepLinkAppleHelper.handle, etc.).
 * Here we simulate it with a button press.
 */
@Composable
fun SchemeHandlerDemo() {
    val lastLink by DeepLinkHandler.lastReceived.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Scheme Handler Demo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Simulates receiving a deep link from the OS.",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = {
                DeepLinkHandler.handle("demo://open/product/42?ref=banner&tab=reviews#section1")
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Simulate: demo://open/product/42?ref=banner")
        }

        Button(
            onClick = { DeepLinkHandler.clear() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear")
        }

        lastLink?.let { link ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("scheme: ${link.scheme}", style = MaterialTheme.typography.bodySmall)
                    Text("host: ${link.host}", style = MaterialTheme.typography.bodySmall)
                    Text("path: ${link.path}", style = MaterialTheme.typography.bodySmall)
                    Text("segments: ${link.pathSegments}", style = MaterialTheme.typography.bodySmall)
                    Text("query: ${link.queryParams}", style = MaterialTheme.typography.bodySmall)
                    Text("fragment: ${link.fragment}", style = MaterialTheme.typography.bodySmall)
                }
            }
        } ?: Text("No link received yet.", style = MaterialTheme.typography.bodySmall)
    }
}
