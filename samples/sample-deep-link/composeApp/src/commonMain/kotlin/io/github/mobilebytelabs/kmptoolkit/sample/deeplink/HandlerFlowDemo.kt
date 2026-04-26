package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLink
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler

/**
 * Demonstrates collecting the [DeepLinkHandler.incoming] SharedFlow as Compose state.
 *
 * Shows a live stream of all received links — mimics how a real app would
 * react to incoming deep links in a navigation host or ViewModel.
 */
@Composable
fun HandlerFlowDemo() {
    val receivedLinks = remember { mutableStateListOf<DeepLink>() }

    // Collect the SharedFlow and accumulate into the list
    LaunchedEffect(Unit) {
        DeepLinkHandler.incoming.collect { link ->
            receivedLinks.add(0, link) // newest first
        }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Handler Flow Demo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Collects DeepLinkHandler.incoming as Compose state.",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = { DeepLinkHandler.handle("demo://open/product/${(1..999).random()}") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Emit random product link") }

        Button(
            onClick = { DeepLinkHandler.handle("demo://open/user/rajan/posts?tab=videos") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Emit profile link") }

        Button(
            onClick = { receivedLinks.clear() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Clear history") }

        Text("Received (${receivedLinks.size}):", style = MaterialTheme.typography.labelSmall)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(receivedLinks) { link ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        link.raw,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
