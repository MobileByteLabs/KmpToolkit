package io.github.template.sample.appupdate.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.appupdate.AppVersion
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateInfo
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType

/**
 * Update Dialog - Shows when an app update is available.
 *
 * Demonstrates real-world usage of update prompts with:
 * - Version comparison (current -> new)
 * - Release notes preview
 * - Multiple action options (Update Now, Later, Skip)
 *
 * Usage:
 * ```kotlin
 * if (showUpdateDialog && updateInfo != null) {
 *     UpdateDialog(
 *         updateInfo = updateInfo,
 *         onUpdateNow = { /* trigger immediate update */ },
 *         onUpdateLater = { /* schedule flexible update */ },
 *         onSkip = { /* dismiss dialog */ },
 *         onDismiss = { showUpdateDialog = false }
 *     )
 * }
 * ```
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onUpdateNow: () -> Unit,
    onUpdateLater: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isImmediate = updateInfo.updateType == UpdateType.IMMEDIATE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                // Version comparison
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "v${updateInfo.currentVersion}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        Text(
                            text = "->",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        Column {
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "v${updateInfo.latestVersion ?: "?.?.?"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Update type indicator
                val updateTypeText = when (updateInfo.updateType) {
                    UpdateType.IMMEDIATE -> "Critical Update Required"
                    UpdateType.FLEXIBLE -> "Recommended Update"
                    UpdateType.NONE -> "Update Available"
                }

                Text(
                    text = updateTypeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isImmediate) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Release notes preview
                updateInfo.releaseNotes?.let { notes ->
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notes.take(200) + if (notes.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdateNow) {
                Text("Update Now")
            }
        },
        dismissButton = {
            Row {
                if (!isImmediate) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                    OutlinedButton(onClick = onUpdateLater) {
                        Text("Later")
                    }
                }
            }
        },
    )
}

/**
 * Simulated UpdateDialog for demo purposes.
 *
 * Use this when you want to show the dialog UI without real update data.
 */
@Composable
fun UpdateDialogPreview(
    onDismiss: () -> Unit,
) {
    val mockUpdateInfo = UpdateInfo.available(
        currentVersion = AppVersion.of(1, 5, 0),
        latestVersion = AppVersion.of(2, 0, 0),
        updateType = UpdateType.FLEXIBLE,
        releaseNotes = """
            - New Clipboard module with cross-platform support
            - Improved App Update UX with better dialogs
            - Bug fixes and performance improvements
            - Added support for more platforms
        """.trimIndent(),
    )

    UpdateDialog(
        updateInfo = mockUpdateInfo,
        onUpdateNow = onDismiss,
        onUpdateLater = onDismiss,
        onSkip = onDismiss,
        onDismiss = onDismiss,
    )
}
