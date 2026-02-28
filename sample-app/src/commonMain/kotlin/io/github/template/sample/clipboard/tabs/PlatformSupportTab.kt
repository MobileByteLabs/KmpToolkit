package io.github.template.sample.clipboard.tabs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.template.sample.appupdate.components.getPlatformName

/**
 * Platform Support Tab - Shows clipboard support matrix across platforms.
 *
 * Displays which clipboard operations are supported on each platform
 * and highlights the current platform.
 */
@Composable
fun PlatformSupportTab() {
    val currentPlatform = getPlatformName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Platform Support Matrix",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Clipboard support varies by platform. Here's what works where:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Current platform indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Current Platform: ",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = currentPlatform,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Support matrix
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Header
                PlatformRow(
                    platform = "Platform",
                    copy = "Copy",
                    read = "Read",
                    notes = "Notes",
                    isHeader = true,
                )

                HorizontalDivider()

                // Platforms
                PlatformRow(
                    platform = "Android",
                    copy = true,
                    read = true,
                    notes = "Full support",
                    isCurrentPlatform = currentPlatform.contains("Android"),
                )

                PlatformRow(
                    platform = "iOS",
                    copy = true,
                    read = true,
                    notes = "Full support",
                    isCurrentPlatform = currentPlatform.contains("iOS"),
                )

                PlatformRow(
                    platform = "macOS",
                    copy = true,
                    read = true,
                    notes = "Full support",
                    isCurrentPlatform = currentPlatform.contains("Mac") ||
                        currentPlatform.contains("mac"),
                )

                PlatformRow(
                    platform = "Windows",
                    copy = true,
                    read = true,
                    notes = "Full support",
                    isCurrentPlatform = currentPlatform.contains("Windows"),
                )

                PlatformRow(
                    platform = "Linux",
                    copy = true,
                    read = true,
                    notes = "Requires xclip/xsel",
                    isCurrentPlatform = currentPlatform.contains("Linux"),
                )

                PlatformRow(
                    platform = "JVM Desktop",
                    copy = true,
                    read = true,
                    notes = "Uses AWT Toolkit",
                    isCurrentPlatform = currentPlatform.contains("JVM") ||
                        currentPlatform.contains("Desktop"),
                )

                PlatformRow(
                    platform = "JS Browser",
                    copy = true,
                    read = false,
                    notes = "Async API, write-only",
                    isCurrentPlatform = currentPlatform.contains("Web") ||
                        currentPlatform.contains("Browser"),
                )

                PlatformRow(
                    platform = "Wasm",
                    copy = true,
                    read = false,
                    notes = "Same as JS",
                    isCurrentPlatform = currentPlatform.contains("Wasm"),
                )

                PlatformRow(
                    platform = "WASI",
                    copy = false,
                    read = false,
                    notes = "No clipboard in WASI",
                    isCurrentPlatform = currentPlatform.contains("WASI"),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Text(
            text = "Legend",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendItem(supported = true, label = "Supported")
            LegendItem(supported = false, label = "Not Supported")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Important Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "- Android auto-initializes via ContentProvider - no setup needed!\n" +
                        "- Web browsers restrict clipboard read for security\n" +
                        "- Linux requires xclip or xsel installed\n" +
                        "- All platforms support synchronous copy operation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlatformRow(
    platform: String,
    copy: String,
    read: String,
    notes: String,
    isHeader: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = platform,
            modifier = Modifier.weight(1.5f),
            style = if (isHeader) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = copy,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        Text(
            text = read,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        Text(
            text = notes,
            modifier = Modifier.weight(2f),
            style = if (isHeader) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PlatformRow(
    platform: String,
    copy: Boolean,
    read: Boolean,
    notes: String,
    isCurrentPlatform: Boolean = false,
) {
    val backgroundColor = if (isCurrentPlatform) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isCurrentPlatform) "$platform *" else platform,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCurrentPlatform) FontWeight.Bold else FontWeight.Normal,
        )
        SupportIndicator(
            supported = copy,
            modifier = Modifier.weight(0.8f),
        )
        SupportIndicator(
            supported = read,
            modifier = Modifier.weight(0.8f),
        )
        Text(
            text = notes,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SupportIndicator(
    supported: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (supported) "Yes" else "No",
            color = if (supported) Color(0xFF4CAF50) else Color(0xFFF44336),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LegendItem(supported: Boolean, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (supported) "Yes" else "No",
            color = if (supported) Color(0xFF4CAF50) else Color(0xFFF44336),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "= $label",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
