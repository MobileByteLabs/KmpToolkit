package io.github.template.sample.appupdate.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdate
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateResult
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.appupdate.VersionInfo
import com.mobilebytelabs.kmptoolkit.appupdate.VersionResolver
import io.github.template.sample.appupdate.components.UpdateResultCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom resolver demonstration.
 *
 * Shows:
 * - Implementing VersionResolver interface
 * - Simulated network delay
 * - Custom version info construction
 */
@Composable
fun CustomResolverTab() {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf("2.0.0") }
    var releaseNotes by remember { mutableStateOf("Bug fixes and improvements") }
    var updateType by remember { mutableStateOf("FLEXIBLE") }
    var isLoading by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Simulated Response Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Simulated Server Response",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Version") },
                    placeholder = { Text("2.0.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Release Notes") },
                    placeholder = { Text("What's new...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = updateType,
                    onValueChange = { updateType = it },
                    label = { Text("Update Type") },
                    placeholder = { Text("FLEXIBLE, IMMEDIATE, or NONE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Action Button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true

                    // Create custom resolver with simulated response
                    val customResolver = object : VersionResolver {
                        override suspend fun resolve(): VersionInfo {
                            // Simulate network delay
                            delay(1000)

                            return VersionInfo(
                                version = version,
                                updateType = when (updateType.uppercase()) {
                                    "IMMEDIATE" -> UpdateType.IMMEDIATE
                                    "NONE" -> UpdateType.NONE
                                    else -> UpdateType.FLEXIBLE
                                },
                                releaseNotes = releaseNotes,
                                downloadUrl = "https://example.com/app-$version.apk",
                                storeUrl = "https://play.google.com/store/apps/details?id=com.example",
                                metadata = mapOf(
                                    "source" to "custom",
                                ),
                            )
                        }
                    }

                    val config = AppUpdateConfig.builder()
                        .versionResolver(customResolver)
                        .build()

                    updateResult = AppUpdate.checkForUpdate(config)
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && version.isNotBlank(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Test Custom Resolver")
            }
        }

        // Result Display
        updateResult?.let { result ->
            UpdateResultCard(result = result)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Code Example Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom Resolver Example",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
class MyVersionResolver : VersionResolver {
    override suspend fun resolve(): VersionInfo {
        // Fetch from your backend
        val response = httpClient.get("api/version")

        return VersionInfo(
            version = response.version,
            updateType = UpdateType.FLEXIBLE,
            releaseNotes = response.notes,
            downloadUrl = response.downloadUrl,
        )
    }
}

// Usage
val config = AppUpdateConfig.builder()
    .versionResolver(MyVersionResolver())
    .build()

val result = AppUpdate.checkForUpdate(config)
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}
