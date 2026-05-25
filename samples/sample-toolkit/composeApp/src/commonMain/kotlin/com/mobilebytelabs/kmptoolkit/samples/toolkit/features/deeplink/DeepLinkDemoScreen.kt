/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.SetupRequiredCard

@Composable
fun DeepLinkDemoScreen(onStatus: (String) -> Unit) {
    LaunchedEffect(Unit) { onStatus("See setup + DSL example below") }

    DemoIntro("Typed deep-link parser with kotlinx.serialization. Declare a data class per route; the parser binds path + query params automatically.")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("DSL example", style = MaterialTheme.typography.titleSmall)
            Text(
                text = """
                    @Serializable
                    data class Product(val id: String)

                    val parser = deepLinkParser {
                        route<Product>("/product/{id}")
                    }
                    val parsed: Product? = parser.parse(
                        DeepLink("app://product/widget-42")
                    )
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
        }
    }

    SetupRequiredCard(
        title = "Wire intent-filter / Universal Links",
        explanation = "Parsing is in-process; OS-level deep-link delivery requires per-platform manifest/entitlement wiring.",
        setupSteps = listOf(
            "Android: declare <intent-filter android:scheme=\"app\" .../> in AndroidManifest",
            "iOS: add Associated Domains entitlement + apple-app-site-association",
            "Web: handle navigation events in your SPA router",
        ),
    )
}
