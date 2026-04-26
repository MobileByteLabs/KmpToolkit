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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLink
import com.mobilebytelabs.kmptoolkit.deeplink.deepLinkParser
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Route data classes — must be @Serializable; field names map to URI params
// ---------------------------------------------------------------------------

@Serializable
data class ProductRoute(val id: String)

@Serializable
data class ProfileRoute(val username: String, val tab: String = "posts")

@Serializable
data class SearchRoute(val q: String, val lang: String = "en")

// ---------------------------------------------------------------------------
// Demo composable
// ---------------------------------------------------------------------------

private data class RouteExample(val label: String, val uri: String, val note: String = "")

private val ROUTE_EXAMPLES = listOf(
    RouteExample("Product /product/99", "demo://open/product/99", "matches ProductRoute(id)"),
    RouteExample("Profile /user/rajan/posts", "demo://open/user/rajan/posts", "default tab=posts"),
    RouteExample("Profile + query ?tab=videos", "demo://open/user/rajan/posts?tab=videos", "query overrides default"),
    RouteExample("Search ?q=kmp", "demo://open/search?q=kotlin+multiplatform", "matches SearchRoute"),
    RouteExample("No match: /about", "demo://open/about", "→ all parsers return null"),
)

/**
 * Demonstrates the [deepLinkParser] DSL for type-safe, zero-reflection route matching.
 *
 * A [com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkParser] is built once (at app startup
 * in production). Each incoming [DeepLink] is matched against registered patterns and
 * decoded into a typed data class via [kotlinx.serialization].
 */
@Composable
fun RouteParserDemo() {
    val parser = remember {
        deepLinkParser {
            route<ProductRoute>("/product/{id}")
            route<ProfileRoute>("/user/{username}/posts")
            route<SearchRoute>("/search")
        }
    }

    var productResult by remember { mutableStateOf<ProductRoute?>(null) }
    var profileResult by remember { mutableStateOf<ProfileRoute?>(null) }
    var searchResult by remember { mutableStateOf<SearchRoute?>(null) }
    var noMatchUri by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    fun parse(uri: String) {
        val link = DeepLink.parse(uri)
        productResult = parser.parse(link)
        profileResult = parser.parse(link)
        searchResult = parser.parse(link)
        noMatchUri = if (productResult == null && profileResult == null && searchResult == null) uri else null
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Route Parser", style = MaterialTheme.typography.titleMedium)

        // Registered routes
        Text("Registered routes", style = MaterialTheme.typography.labelMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RouteEntry("ProductRoute", "/product/{id}", "id: String")
                RouteEntry("ProfileRoute", "/user/{username}/posts", "username: String, tab: String = \"posts\"")
                RouteEntry("SearchRoute", "/search", "q: String, lang: String = \"en\"")
            }
        }

        HorizontalDivider()
        Text("Try these URIs", style = MaterialTheme.typography.labelMedium)

        ROUTE_EXAMPLES.forEach { example ->
            Button(
                onClick = { parse(example.uri) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(example.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        example.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                }
            }
        }

        HorizontalDivider()
        Text("Match result", style = MaterialTheme.typography.labelMedium)

        when {
            productResult != null -> MatchCard("ProductRoute") {
                ResultRow("id", productResult!!.id)
            }

            profileResult != null -> MatchCard("ProfileRoute") {
                ResultRow("username", profileResult!!.username)
                ResultRow("tab", profileResult!!.tab)
            }

            searchResult != null -> MatchCard("SearchRoute") {
                ResultRow("q", searchResult!!.q)
                ResultRow("lang", searchResult!!.lang)
            }

            noMatchUri != null -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "No match",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "\"$noMatchUri\" did not match any registered route.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            else -> Text(
                "Tap a URI above to see the matched route.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteEntry(typeName: String, pattern: String, fields: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                pattern,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            Text("→ $typeName", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            fields,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun MatchCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ResultRow(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$key =", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
