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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardFilter
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardMonitor
import com.mobilebytelabs.kmptoolkit.clipboard.UrlDetection
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardMonitor
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ClipboardMonitorScreen() {
    val monitor = remember { createClipboardMonitor() }
    val state by monitor.state.collectAsState()
    val latestChange by monitor.latestChange.collectAsState()
    val changes = remember { mutableStateListOf<String>() }
    val urlDetections = remember { mutableStateListOf<String>() }

    // Collect changes and URL detections
    DisposableEffect(monitor) {
        val scope = CoroutineScope(Dispatchers.Main)

        // Setup matchers
        SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }

        val changesJob = scope.launch {
            monitor.changes.collect { change ->
                val entry = "[${change.contentType}] ${change.content.take(60)}"
                changes.add(0, entry)
                if (changes.size > 20) changes.removeAt(changes.lastIndex)
            }
        }

        val urlJob = scope.launch {
            monitor.urlDetections.collect { detection ->
                val entry = "${detection.matcher.name}: ${detection.url.take(50)}"
                urlDetections.add(0, entry)
                if (urlDetections.size > 10) urlDetections.removeAt(urlDetections.lastIndex)
            }
        }

        onDispose {
            changesJob.cancel()
            urlJob.cancel()
            monitor.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Clipboard Monitor Demo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Continuous monitoring + URL detection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Monitor State Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (state) {
                    is ClipboardMonitorState.Monitoring -> MaterialTheme.colorScheme.primaryContainer
                    is ClipboardMonitorState.Paused -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Monitor State",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (state) {
                        is ClipboardMonitorState.Idle -> "Idle"
                        is ClipboardMonitorState.Monitoring -> "Monitoring"
                        is ClipboardMonitorState.Paused -> "Paused"
                        is ClipboardMonitorState.Error -> "Error: ${(state as ClipboardMonitorState.Error).message}"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)
                },
                enabled = state is ClipboardMonitorState.Idle,
            ) {
                Text("Start")
            }

            OutlinedButton(
                onClick = { monitor.pause() },
                enabled = state is ClipboardMonitorState.Monitoring,
            ) {
                Text("Pause")
            }

            OutlinedButton(
                onClick = { monitor.resume() },
                enabled = state is ClipboardMonitorState.Paused,
            ) {
                Text("Resume")
            }

            OutlinedButton(
                onClick = {
                    monitor.stop()
                    changes.clear()
                    urlDetections.clear()
                },
                enabled = state !is ClipboardMonitorState.Idle,
            ) {
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Latest Change Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Latest Clipboard Change",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = latestChange?.let {
                        "${it.contentType}: ${it.content.take(80)}"
                    } ?: "(no changes detected yet)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // URL Detections Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (urlDetections.isNotEmpty()) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "URL Detections (${urlDetections.size})",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (urlDetections.isEmpty()) {
                    Text(
                        text = "Copy an Instagram/TikTok/YouTube URL to detect it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    urlDetections.forEach { detection ->
                        Text(
                            text = detection,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Change Log Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Change Log (${changes.size})",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (changes.isEmpty()) {
                    Text(
                        text = "Start monitoring and copy something to see changes",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    changes.take(5).forEach { change ->
                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (changes.size > 5) {
                        Text(
                            text = "... and ${changes.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Matchers Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Active URL Matchers",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SocialMediaUrlMatchers.all().joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
