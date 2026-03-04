package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.toast.ClipboardToastHost
import com.mobilebytelabs.kmptoolkit.toast.ToastDuration
import com.mobilebytelabs.kmptoolkit.toast.ToastStyle

/**
 * Demo screen showing clipboard observation functionality.
 *
 * This screen automatically detects clipboard changes, including
 * when content is copied in other apps and the user returns to this app.
 * Shows a toast notification when clipboard content changes.
 */
@Composable
fun ClipboardObserverScreen() {
    val observer = rememberClipboardObserver()
    val clipboardContent by observer.clipboardContent.collectAsState()
    var detectedChanges by remember { mutableStateOf(0) }
    var lastContent by remember { mutableStateOf<String?>(null) }

    // Track changes (only count when content actually changes)
    LaunchedEffect(clipboardContent) {
        if (clipboardContent != lastContent && clipboardContent != null) {
            detectedChanges++
            lastContent = clipboardContent
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Clipboard Observer Demo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Detects clipboard changes automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Observer Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (observer.isObserving) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Observer Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (observer.isObserving) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (observer.isObserving) "Active" else "Stopped",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (observer.isObserving) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clipboard Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Clipboard Content",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = clipboardContent ?: "(empty or unavailable)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (clipboardContent != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Changes Counter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Changes Detected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$detectedChanges",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Platform Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Platform",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getPlatform().name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Text(
                text = "Try copying text in another app, then return here to see the update!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Toast host for clipboard notifications
        ClipboardToastHost(
            clipboardContent = observer.clipboardContent,
            modifier = Modifier.align(Alignment.BottomCenter),
            messageFormatter = { "Copied: $it" },
            duration = ToastDuration.SHORT,
            style = ToastStyle.SUCCESS,
            maxLength = 50,
            showOnlyOnChange = true,
        )
    }
}
