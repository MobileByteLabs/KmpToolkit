/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.networkmonitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectIsOnlineAsState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectNetworkQualityAsState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectNetworkStatusAsState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.rememberNetworkMonitor

@Composable
fun NetworkMonitorDemoScreen(onStatus: (String) -> Unit) {
    val monitor = rememberNetworkMonitor()
    val isOnline by monitor.collectIsOnlineAsState()
    val status by monitor.collectNetworkStatusAsState()
    val quality by monitor.collectNetworkQualityAsState()

    DemoIntro("Reactive connectivity. The chip below auto-updates — disconnect from Wi-Fi to see Offline state.")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (isOnline) "● ONLINE" else "○ OFFLINE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Status: $status", style = MaterialTheme.typography.bodySmall)
            Text("Quality: $quality", style = MaterialTheme.typography.bodySmall)
        }
    }

    Text(
        "Use `requireOnline()`, `ifOnline { }`, `retryOnReconnect(max) { }` extensions for fail-fast / suspend-until-online patterns.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
