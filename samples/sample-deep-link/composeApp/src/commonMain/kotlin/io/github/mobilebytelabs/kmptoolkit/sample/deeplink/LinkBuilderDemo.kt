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
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkBuilder

/**
 * Demonstrates [DeepLinkBuilder] for constructing outbound deep link URIs.
 */
@Composable
fun LinkBuilderDemo() {
    val builder = remember { DeepLinkBuilder(scheme = "demo", host = "open") }
    var builtUri by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Link Builder Demo", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = {
                builtUri = builder.build("/product/{id}", pathParams = mapOf("id" to "42"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Build: /product/42") }

        Button(
            onClick = {
                builtUri = builder.build(
                    "/search",
                    queryParams = mapOf("q" to "hello world", "lang" to "en"),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Build: /search?q=hello+world") }

        Button(
            onClick = {
                builtUri = builder.build(
                    "/user/{username}/posts",
                    pathParams = mapOf("username" to "rajan"),
                    queryParams = mapOf("tab" to "videos"),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Build: /user/rajan/posts?tab=videos") }

        if (builtUri.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Built URI:", style = MaterialTheme.typography.labelSmall)
                    Text(builtUri, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
