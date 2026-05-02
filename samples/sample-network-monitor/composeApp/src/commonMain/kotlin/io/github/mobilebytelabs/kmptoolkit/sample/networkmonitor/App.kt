package io.github.mobilebytelabs.kmptoolkit.sample.networkmonitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.ConnectivityBanner
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectIsOnlineAsState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectNetworkStatusAsState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.rememberScopedNetworkMonitor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val monitor = rememberScopedNetworkMonitor()

    val isOnline by monitor.collectIsOnlineAsState()
    val networkStatus by monitor.collectNetworkStatusAsState()
    val events = remember { mutableStateListOf<String>() }

    LaunchedEffect(monitor) {
        monitor.networkChanges.collect { event ->
            val text = when (event) {
                is NetworkChangeEvent.Connected -> "Connected via ${event.info.type}"
                is NetworkChangeEvent.Disconnected -> "Disconnected"
                is NetworkChangeEvent.TypeChanged -> "Type: ${event.from} -> ${event.to}"
                is NetworkChangeEvent.MeteredChanged -> "Metered: ${event.isMetered}"
                is NetworkChangeEvent.CaptivePortalDetected -> "Captive portal: ${event.redirectUrl}"
                is NetworkChangeEvent.CaptivePortalResolved -> "Captive portal resolved"
            }
            events.add(0, text)
            if (events.size > 50) events.removeLast()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Network Monitor") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Connectivity banner from compose module
                    ConnectivityBanner(monitor = monitor)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Status card
                        item {
                            StatusCard(isOnline = isOnline, status = networkStatus)
                        }

                        // Info card
                        item {
                            when (val status = networkStatus) {
                                is NetworkStatus.Available -> {
                                    NetworkInfoCard(status = status)
                                }

                                is NetworkStatus.CaptivePortal -> {
                                    CaptivePortalCard(redirectUrl = status.redirectUrl)
                                }

                                is NetworkStatus.Unavailable -> { /* handled by ConnectivityBanner */ }
                            }
                        }

                        // Events header
                        item {
                            Text(
                                text = "Events",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (events.isEmpty()) {
                            item {
                                Text(
                                    text = "No events yet. Toggle WiFi/airplane mode to see events.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(events) { event ->
                            EventItem(event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(isOnline: Boolean, status: NetworkStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) {
                Color(0xFF4CAF50).copy(alpha = 0.12f)
            } else {
                Color(0xFFF44336).copy(alpha = 0.12f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336),
                )
                Text(
                    text = when (status) {
                        is NetworkStatus.Available -> "Connected via ${status.info.type}"
                        is NetworkStatus.CaptivePortal -> "Captive portal detected"
                        is NetworkStatus.Unavailable -> "No network connection"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NetworkInfoCard(status: NetworkStatus.Available) {
    val info = status.info
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Network Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            InfoRow("Type", info.type.name)
            InfoRow("Metered", if (info.isMetered) "Yes" else "No")
            if (info.downstreamBandwidthKbps > 0) {
                InfoRow("Download", "${info.downstreamBandwidthKbps} kbps")
            }
            if (info.upstreamBandwidthKbps > 0) {
                InfoRow("Upload", "${info.upstreamBandwidthKbps} kbps")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EventItem(event: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Text(
            text = event,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CaptivePortalCard(redirectUrl: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Captive Portal Detected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800),
            )
            Text(
                text = redirectUrl ?: "Sign in required to access the internet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
