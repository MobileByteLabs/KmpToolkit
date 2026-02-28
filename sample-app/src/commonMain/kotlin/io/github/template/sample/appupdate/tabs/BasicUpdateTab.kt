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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.mobilebytelabs.kmptoolkit.appupdate.AppVersion
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateResult
import io.github.template.sample.appupdate.components.PlatformBadge
import io.github.template.sample.appupdate.components.UpdateResultCard
import kotlinx.coroutines.launch

/**
 * Basic update check demonstration.
 *
 * Shows:
 * - Current app version
 * - Platform detection
 * - Basic update check with default config
 * - Store redirect functionality
 */
@Composable
fun BasicUpdateTab() {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var currentVersion by remember { mutableStateOf<AppVersion?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Platform Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Platform Info",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Current Platform:")
                    PlatformBadge()
                }
                currentVersion?.let { version ->
                    Text("App Version: ${version.versionName}")
                    version.versionCode?.let { code ->
                        Text("Version Code: $code")
                    }
                }
            }
        }

        // Actions Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                )

                Button(
                    onClick = {
                        scope.launch {
                            currentVersion = AppUpdate.getCurrentVersion()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Get Current Version")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            updateResult = AppUpdate.checkForUpdate()
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Check for Update")
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            AppUpdate.openStoreForUpdate(AppUpdateConfig.Default)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Store")
                }
            }
        }

        // Result Card
        updateResult?.let { result ->
            UpdateResultCard(result = result)
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
                    text = "About Basic Updates",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        The basic update check uses platform defaults:

                        • Android: Google Play In-App Updates
                        • iOS: App Store version lookup
                        • macOS: Mac App Store
                        • Desktop: Requires custom resolver

                        For desktop platforms, configure a version resolver
                        (GitHub, Supabase, or custom) to enable update checks.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
