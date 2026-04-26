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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.deeplink.deepLinkParser
import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser
import kotlinx.serialization.Serializable

@Serializable
data class ProductRoute(val id: String)

@Serializable
data class ProfileRoute(val username: String, val tab: String = "posts")

/**
 * Demonstrates the [deepLinkParser] DSL for type-safe route matching.
 */
@Composable
fun RouteParserDemo() {
    val parser = remember {
        deepLinkParser {
            route<ProductRoute>("/product/{id}")
            route<ProfileRoute>("/user/{username}/posts")
        }
    }

    var productResult by remember { mutableStateOf<ProductRoute?>(null) }
    var profileResult by remember { mutableStateOf<ProfileRoute?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Route Parser Demo", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = {
                val link = UriParser.parse("demo://open/product/99")
                productResult = parser.parse(link)
                profileResult = null
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Parse: /product/99") }

        Button(
            onClick = {
                val link = UriParser.parse("demo://open/user/rajan/posts?tab=videos")
                profileResult = parser.parse(link)
                productResult = null
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Parse: /user/rajan/posts?tab=videos") }

        productResult?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ProductRoute matched", style = MaterialTheme.typography.labelSmall)
                    Text("id = ${it.id}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        profileResult?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ProfileRoute matched", style = MaterialTheme.typography.labelSmall)
                    Text("username = ${it.username}", style = MaterialTheme.typography.bodySmall)
                    Text("tab = ${it.tab}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
