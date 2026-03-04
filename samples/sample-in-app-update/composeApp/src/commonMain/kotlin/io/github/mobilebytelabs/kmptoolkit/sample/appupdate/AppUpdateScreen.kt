package io.github.mobilebytelabs.kmptoolkit.sample.appupdate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdate
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateInfo
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateResult
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.toast.ToastDuration
import com.mobilebytelabs.kmptoolkit.toast.ToastHostState
import com.mobilebytelabs.kmptoolkit.toast.ToastStyle
import kotlinx.coroutines.launch

/**
 * Demo screen showing In-App Update functionality.
 *
 * Demonstrates:
 * - Checking for updates with GitHubResolver
 * - Configuring update behavior
 * - Displaying update information
 * - Handling update results
 */
@Composable
fun AppUpdateScreen(toastState: ToastHostState) {
    val scope = rememberCoroutineScope()

    // Demo configuration - Using a public GitHub repo for demo
    var owner by remember { mutableStateOf("nicklockwood") }
    var repo by remember { mutableStateOf("SwiftFormat") }

    // State
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var currentVersion by remember { mutableStateOf(AppUpdate.getCurrentVersion().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "In-App Update Demo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check for updates using GitHub Releases",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("GitHub Owner") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("Repository") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Current App Version: $currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check Button
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isChecking = true
                        lastError = null
                        updateInfo = null

                        try {
                            val config = AppUpdateConfig.builder()
                                .github(owner, repo)
                                .build()

                            when (val result = AppUpdate.checkForUpdate(config)) {
                                is UpdateResult.Success -> {
                                    updateInfo = result.updateInfo
                                    val info = result.updateInfo
                                    toastState.showToast(
                                        message = when (info.updateType) {
                                            UpdateType.NONE -> "You're up to date!"
                                            UpdateType.FLEXIBLE -> "Optional update available"
                                            UpdateType.IMMEDIATE -> "Critical update available!"
                                        },
                                        duration = ToastDuration.MEDIUM,
                                        style = when (info.updateType) {
                                            UpdateType.NONE -> ToastStyle.SUCCESS
                                            UpdateType.FLEXIBLE -> ToastStyle.INFO
                                            UpdateType.IMMEDIATE -> ToastStyle.WARNING
                                        },
                                    )
                                }

                                is UpdateResult.Error -> {
                                    lastError = result.message
                                    toastState.showToast(
                                        message = "Error: ${result.message}",
                                        duration = ToastDuration.LONG,
                                        style = ToastStyle.ERROR,
                                    )
                                }

                                is UpdateResult.NotSupported -> {
                                    lastError = result.reason
                                    toastState.showToast(
                                        message = "Not supported: ${result.reason}",
                                        duration = ToastDuration.MEDIUM,
                                        style = ToastStyle.WARNING,
                                    )
                                }

                                is UpdateResult.Cancelled -> {
                                    toastState.showToast(
                                        message = "Update check cancelled",
                                        duration = ToastDuration.SHORT,
                                        style = ToastStyle.DEFAULT,
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            lastError = e.message ?: "Unknown error"
                            toastState.showToast(
                                message = "Exception: ${e.message}",
                                duration = ToastDuration.LONG,
                                style = ToastStyle.ERROR,
                            )
                        }

                        isChecking = false
                    }
                },
                enabled = !isChecking && owner.isNotBlank() && repo.isNotBlank(),
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isChecking) "Checking..." else "Check for Update")
            }

            OutlinedButton(
                onClick = {
                    updateInfo = null
                    lastError = null
                },
            ) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result Card
        updateInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (info.updateType) {
                        UpdateType.NONE -> MaterialTheme.colorScheme.primaryContainer
                        UpdateType.FLEXIBLE -> MaterialTheme.colorScheme.secondaryContainer
                        UpdateType.IMMEDIATE -> MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (info.updateType) {
                            UpdateType.NONE -> "Up to Date"
                            UpdateType.FLEXIBLE -> "Optional Update Available"
                            UpdateType.IMMEDIATE -> "Critical Update Required"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Current: ${info.currentVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    info.latestVersion?.let { latest ->
                        Text(
                            text = "Latest: $latest",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    info.releaseNotes?.let { notes ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Release Notes:",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = notes.take(500) + if (notes.length > 500) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    info.storeUrl?.let { url ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Download: $url",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Error Card
        lastError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
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

        // Usage Info
        Text(
            text = "Try changing the version to an older one (e.g., 0.50.0) to see update detection!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
