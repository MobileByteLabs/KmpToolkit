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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkBuilder
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler

private data class BuildExample(
    val label: String,
    val pathPattern: String,
    val pathParams: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
)

private val BUILD_EXAMPLES = listOf(
    BuildExample(
        label = "Product detail",
        pathPattern = "/product/{id}",
        pathParams = mapOf("id" to "42"),
    ),
    BuildExample(
        label = "Search with query",
        pathPattern = "/search",
        queryParams = mapOf("q" to "kotlin multiplatform", "lang" to "en"),
    ),
    BuildExample(
        label = "Profile + query param",
        pathPattern = "/user/{username}/posts",
        pathParams = mapOf("username" to "rajan"),
        queryParams = mapOf("tab" to "videos"),
    ),
    BuildExample(
        label = "Promo with spaces (encoded)",
        pathPattern = "/promo/{slug}",
        pathParams = mapOf("slug" to "summer sale"),
        queryParams = mapOf("code" to "SAVE 20"),
    ),
)

/**
 * Demonstrates [DeepLinkBuilder] for constructing properly-encoded outbound deep link URIs.
 *
 * The builder handles:
 * - Path parameter substitution: `{id}` → value
 * - Query string encoding: spaces → `%20`, special chars escaped
 * - Fragment appending
 * - Round-trip: built URIs can be immediately fired through [DeepLinkHandler] to verify
 *   the Stream and Scheme tabs receive them correctly.
 */
@Composable
fun LinkBuilderDemo() {
    val builder = remember { DeepLinkBuilder(scheme = "demo", host = "open") }
    var builtUri by remember { mutableStateOf<String?>(null) }
    var firedUri by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Link Builder", style = MaterialTheme.typography.titleMedium)
        Text(
            "Build outbound URIs with path params, query strings, and fragments. " +
                "Tap \"Fire\" to send through DeepLinkHandler and watch the Scheme + Stream tabs update.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Builder config display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Builder config", style = MaterialTheme.typography.labelSmall)
                Text(
                    "scheme = \"demo\"  host = \"open\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }

        HorizontalDivider()
        Text("Examples", style = MaterialTheme.typography.labelMedium)

        BUILD_EXAMPLES.forEach { example ->
            Button(
                onClick = {
                    builtUri = builder.build(
                        pathPattern = example.pathPattern,
                        pathParams = example.pathParams,
                        queryParams = example.queryParams,
                    )
                    firedUri = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(example.label, style = MaterialTheme.typography.labelMedium)
                    val hint = buildString {
                        if (example.pathParams.isNotEmpty()) append("path: ${example.pathParams}  ")
                        if (example.queryParams.isNotEmpty()) append("query: ${example.queryParams}")
                    }
                    if (hint.isNotBlank()) {
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Result + round-trip
        if (builtUri != null) {
            Text("Built URI", style = MaterialTheme.typography.labelMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(
                    builtUri!!,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        builtUri = null
                        firedUri = null
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Clear") }

                Button(
                    onClick = {
                        DeepLinkHandler.handle(builtUri!!)
                        firedUri = builtUri
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Fire →") }
            }

            if (firedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Fired through DeepLinkHandler", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "Check the Scheme and Stream tabs to see it arrive.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            Text(
                "Tap an example above to build a URI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
