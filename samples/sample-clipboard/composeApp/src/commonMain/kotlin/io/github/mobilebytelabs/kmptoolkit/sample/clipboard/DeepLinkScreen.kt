package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLink
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkBuilder
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler
import com.mobilebytelabs.kmptoolkit.deeplink.deepLinkParser
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Route models for the parser demo
// ---------------------------------------------------------------------------

@Serializable
private data class DemoProductRoute(val id: String)

@Serializable
private data class DemoProfileRoute(val username: String, val tab: String = "posts")

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun DeepLinkScreen() {
    val lastLink by DeepLinkHandler.lastReceived.collectAsState()
    val streamLinks = remember { mutableStateListOf<DeepLink>() }
    var routeResult by remember { mutableStateOf("") }
    var builtUri by remember { mutableStateOf("") }

    // Collect the SharedFlow live stream
    LaunchedEffect(Unit) {
        DeepLinkHandler.incoming.collect { link ->
            streamLinks.add(0, link)
        }
    }

    val parser = remember {
        deepLinkParser {
            route<DemoProductRoute>("/product/{id}")
            route<DemoProfileRoute>("/user/{username}/posts")
        }
    }

    val builder = remember { DeepLinkBuilder(scheme = "demo", host = "open") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Tap any section to try the cmp-deep-link API.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ----------------------------------------------------------------
        // 1. Handle URIs
        // ----------------------------------------------------------------
        DeepLinkSectionCard(
            emoji = "\uD83D\uDD17",
            title = "Handle URI",
            description = "DeepLinkHandler.handle(uri)",
            color = Color(0xFF1565C0),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "demo://open/product/42?ref=banner" to "Product",
                    "demo://open/user/rajan/posts?tab=videos" to "Profile",
                    "https://example.com/promo?code=KMP" to "HTTPS",
                ).forEach { (uri, label) ->
                    Button(
                        onClick = { DeepLinkHandler.handle(uri) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("$label  →  $uri", maxLines = 1, color = Color.White, fontSize = 12.sp)
                    }
                }
                if (lastLink != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DeepLinkResultSurface(
                        label = "lastReceived",
                        value = buildString {
                            appendLine("scheme = ${lastLink!!.scheme}")
                            appendLine("host   = ${lastLink!!.host}")
                            appendLine("path   = ${lastLink!!.path}")
                            if (lastLink!!.queryParams.isNotEmpty()) {
                                appendLine("query  = ${lastLink!!.queryParams}")
                            }
                        }.trimEnd(),
                        isError = false,
                        onDismiss = { DeepLinkHandler.clear() },
                    )
                }
            }
        }

        // ----------------------------------------------------------------
        // 2. Route Parser (type-safe DSL)
        // ----------------------------------------------------------------
        DeepLinkSectionCard(
            emoji = "\uD83E\uDDED",
            title = "Route Parser",
            description = "deepLinkParser { route<T>(pattern) }",
            color = Color(0xFF6A1B9A),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "demo://open/product/99" to "ProductRoute",
                    "demo://open/user/rajan/posts?tab=videos" to "ProfileRoute",
                    "demo://open/unknown/path" to "No match",
                ).forEach { (uri, expectedType) ->
                    Button(
                        onClick = {
                            val link = DeepLink.parse(uri)
                            val product: DemoProductRoute? = parser.parse(link)
                            val profile: DemoProfileRoute? = parser.parse(link)
                            routeResult = when {
                                product != null -> "ProductRoute → id=\"${product.id}\""

                                profile != null ->
                                    "ProfileRoute → username=\"${profile.username}\", tab=\"${profile.tab}\""

                                else -> "No route matched for \"$uri\""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Parse → $expectedType", color = Color.White, fontSize = 12.sp)
                    }
                }
                if (routeResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DeepLinkResultSurface(
                        label = "parse result",
                        value = routeResult,
                        isError = routeResult.startsWith("No route"),
                        onDismiss = { routeResult = "" },
                    )
                }
            }
        }

        // ----------------------------------------------------------------
        // 3. Link Builder
        // ----------------------------------------------------------------
        DeepLinkSectionCard(
            emoji = "\uD83D\uDD28",
            title = "Link Builder",
            description = "DeepLinkBuilder(scheme, host).build(pattern, params)",
            color = Color(0xFF2E7D32),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("/product/{id}", mapOf("id" to "42"), emptyMap<String, String>()),
                    Triple("/user/{username}/posts", mapOf("username" to "rajan"), mapOf("tab" to "videos")),
                    Triple("/search", emptyMap(), mapOf("q" to "kotlin multiplatform")),
                ).forEach { (pattern, path, query) ->
                    Button(
                        onClick = { builtUri = builder.build(pattern, path, query) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Build \"$pattern\"",
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (builtUri.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DeepLinkResultSurface(
                        label = "built URI",
                        value = builtUri,
                        isError = false,
                        onDismiss = { builtUri = "" },
                        extraAction = "Fire →" to { DeepLinkHandler.handle(builtUri) },
                    )
                }
            }
        }

        // ----------------------------------------------------------------
        // 4. Live stream
        // ----------------------------------------------------------------
        DeepLinkSectionCard(
            emoji = "\uD83D\uDCE1",
            title = "Incoming Stream",
            description = "DeepLinkHandler.incoming: SharedFlow<DeepLink>",
            color = Color(0xFFBF360C),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { DeepLinkHandler.handle("demo://open/product/${(1..999).random()}") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF360C)),
                        modifier = Modifier.weight(1f),
                    ) { Text("Emit random", color = Color.White, fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { streamLinks.clear() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Clear", fontSize = 12.sp) }
                }
                if (streamLinks.isEmpty()) {
                    Text(
                        "No links yet — tap Emit random or fire from the Handle section.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "${streamLinks.size} link(s) received",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    streamLinks.take(5).forEach { link ->
                        Text(
                            "• ${link.raw}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                    if (streamLinks.size > 5) {
                        Text(
                            "… and ${streamLinks.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Shared card / result composables
// ---------------------------------------------------------------------------

@Composable
private fun DeepLinkSectionCard(
    emoji: String,
    title: String,
    description: String,
    color: Color,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DeepLinkResultSurface(
    label: String,
    value: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    extraAction: Pair<String, () -> Unit>? = null,
) {
    val bgColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val textColor = if (isError) Color(0xFFB71C1C) else Color(0xFF1B5E20)

    Surface(color = bgColor, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isError) "⚠️ $label" else "✅ $label",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = textColor,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.End),
            ) {
                extraAction?.let { (label, action) ->
                    Button(
                        onClick = action,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    ) { Text(label, color = Color.White, fontSize = 12.sp) }
                }
                OutlinedButton(onClick = onDismiss) { Text("Dismiss", fontSize = 12.sp) }
            }
        }
    }
}
