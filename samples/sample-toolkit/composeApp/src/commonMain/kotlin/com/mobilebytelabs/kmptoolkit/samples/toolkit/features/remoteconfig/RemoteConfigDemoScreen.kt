/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.remoteconfig

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
import com.mobilebytelabs.remoteconfig.dynamic.DynamicUiRenderer
import com.mobilebytelabs.remoteconfig.dynamic.model.UiNode
import com.mobilebytelabs.remoteconfig.dynamic.model.UiTextStyle

@Composable
fun RemoteConfigDemoScreen(onStatus: (String) -> Unit) {
    LaunchedEffect(Unit) { onStatus("Rendering a hardcoded UiNode tree (no Supabase needed)") }

    DemoIntro("Remote Config + DynamicUI renderer. The tree below is built inline in code; in production you'd fetch it as JSON from Supabase. Now ships on JVM (3.3.0).")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Live DynamicUiRenderer", style = MaterialTheme.typography.titleSmall)
            DynamicUiRenderer(
                node = UiNode.Column(
                    spacing = 4,
                    children = listOf(
                        UiNode.Text("Welcome 👋", style = UiTextStyle.HEADLINE),
                        UiNode.Text("This UI was declared as a UiNode tree, not Compose code.", style = UiTextStyle.BODY),
                        UiNode.Divider(),
                        UiNode.Badge(text = "JVM 3.3.0", color = null, backgroundColor = "#1565C0"),
                    ),
                ),
                onAction = { action -> onStatus("UiAction → $action") },
            )
        }
    }

    SetupRequiredCard(
        title = "Fetch the tree from a backend",
        explanation = "DynamicUiRenderer is in-process; the cmp-remote-config integration adds Supabase fetch + caching + remote evaluation.",
        setupSteps = listOf(
            "Configure Supabase remote_config table with JSONB UiNode payload",
            "Inject RemoteConfigService(supabaseUrl, supabaseKey) via Koin",
            "Wrap your screen in RemoteConfigHost { node -> DynamicUiRenderer(node, ::handleAction) }",
        ),
    )
}
