package io.github.template.sample.appupdate.tabs

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateResult
import io.github.template.sample.appupdate.components.UpdateResultCard
import io.github.template.sample.appupdate.components.VersionInfoCard
import kotlinx.coroutines.launch

/**
 * GitHub Releases resolver demonstration.
 *
 * Shows:
 * - Configuring owner/repo
 * - Optional token for private repos
 * - Pre-release inclusion toggle
 * - Live version check from GitHub API
 */
@Composable
fun GitHubResolverTab() {
    val scope = rememberCoroutineScope()
    var owner by remember { mutableStateOf("anthropics") }
    var repo by remember { mutableStateOf("claude-code") }
    var token by remember { mutableStateOf("") }
    var includePreRelease by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Configuration Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "GitHub Repository",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner") },
                    placeholder = { Text("username or organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("Repository") },
                    placeholder = { Text("repository-name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token (optional)") },
                    placeholder = { Text("ghp_xxx (for private repos)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includePreRelease,
                        onCheckedChange = { includePreRelease = it },
                    )
                    Text("Include pre-releases")
                }
            }
        }

        // Action Button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null

                    try {
                        val config = AppUpdateConfig.builder()
                            .github(
                                owner = owner,
                                repo = repo,
                                token = token.ifBlank { null },
                            )
                            .build()

                        updateResult = AppUpdate.checkForUpdate(config)
                    } catch (e: Exception) {
                        error = e.message ?: "Unknown error"
                    }

                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && owner.isNotBlank() && repo.isNotBlank(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Check GitHub Release")
            }
        }

        // Error Display
        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Result Display
        updateResult?.let { result ->
            UpdateResultCard(result = result)

            // Show version info if successful
            if (result is UpdateResult.Success && result.updateInfo.isAvailable) {
                result.updateInfo.latestVersion?.let { latestVersion ->
                    VersionInfoCard(
                        version = latestVersion.versionName,
                        releaseNotes = result.updateInfo.releaseNotes,
                        storeUrl = result.updateInfo.storeUrl,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GitHub Resolver Features",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        • Fetches latest release from GitHub API
                        • Auto-detects platform assets (.exe, .dmg, .AppImage)
                        • Extracts version from tag_name (strips 'v' prefix)
                        • Includes release notes from body

                        Rate limits:
                        • Unauthenticated: 60 requests/hour
                        • With token: 5000 requests/hour
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
