package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLink
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler

private val BURST_LINKS = listOf(
    "demo://open/product/10",
    "demo://open/user/alice/posts?tab=photos",
    "demo://open/search?q=kmp",
    "demo://open/product/20?ref=push",
    "demo://open/home",
)

/**
 * Demonstrates collecting [DeepLinkHandler.incoming] as Compose state.
 *
 * Shows every received [DeepLink] in a live list (newest first). Tap any row
 * to expand it and inspect its full parsed components.
 *
 * In production: connect this LaunchedEffect to your ViewModel or navigation host
 * to react to incoming links as they arrive.
 */
@Composable
fun HandlerFlowDemo() {
    val receivedLinks = remember { mutableStateListOf<DeepLink>() }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        DeepLinkHandler.incoming.collect { link ->
            receivedLinks.add(0, link)
            expandedIndex = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Live Stream", style = MaterialTheme.typography.titleMedium)
                if (receivedLinks.isNotEmpty()) {
                    Badge { Text("${receivedLinks.size}") }
                }
            }
            Text(
                "Collects DeepLinkHandler.incoming as Compose state. " +
                    "All tabs share the same singleton — links fired from any tab appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { DeepLinkHandler.handle("demo://open/product/${(1..999).random()}") },
                    modifier = Modifier.weight(1f),
                ) { Text("Random product") }

                Button(
                    onClick = { BURST_LINKS.forEach { DeepLinkHandler.handle(it) } },
                    modifier = Modifier.weight(1f),
                ) { Text("Burst ×5") }
            }

            OutlinedButton(
                onClick = {
                    receivedLinks.clear()
                    expandedIndex = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear history") }

            HorizontalDivider()
        }

        if (receivedLinks.isEmpty()) {
            Text(
                "No links yet. Use the buttons above, or fire a link from the Scheme / Builder tabs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(receivedLinks) { index, link ->
                    val expanded = expandedIndex == index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clickable { expandedIndex = if (expanded) null else index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (expanded) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    link.raw,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                                )
                                Text(
                                    if (expanded) "▲" else "▼",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }

                            AnimatedVisibility(visible = expanded) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    HorizontalDivider()
                                    StreamField("scheme", link.scheme)
                                    StreamField("host", link.host)
                                    StreamField("path", link.path)
                                    if (link.pathSegments.isNotEmpty()) {
                                        StreamField("segments", link.pathSegments.joinToString(" / "))
                                    }
                                    link.queryParams.entries.forEach { (k, v) ->
                                        StreamField("?$k", v)
                                    }
                                    link.fragment?.let { frag ->
                                        StreamField("#fragment", frag)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamField(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
