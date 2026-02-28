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
import io.github.template.sample.appupdate.components.UpdateResultCard
import io.github.template.sample.appupdate.components.VersionInfoCard
import kotlinx.coroutines.launch

/**
 * Supabase resolver demonstration.
 *
 * Shows:
 * - Configuring Supabase project URL and anon key
 * - Custom table name support
 * - Live version check from Supabase REST API
 */
@Composable
fun SupabaseResolverTab() {
    val scope = rememberCoroutineScope()
    var projectUrl by remember { mutableStateOf("") }
    var anonKey by remember { mutableStateOf("") }
    var tableName by remember { mutableStateOf("app_versions") }
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
                    text = "Supabase Configuration",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = projectUrl,
                    onValueChange = { projectUrl = it },
                    label = { Text("Project URL") },
                    placeholder = { Text("https://xxx.supabase.co") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text("Anon Key") },
                    placeholder = { Text("eyJ...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    label = { Text("Table Name") },
                    placeholder = { Text("app_versions") },
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
                    error = null

                    try {
                        val config = AppUpdateConfig.builder()
                            .supabase(
                                projectUrl = projectUrl,
                                anonKey = anonKey,
                                tableName = tableName,
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
            enabled = !isLoading && projectUrl.isNotBlank() && anonKey.isNotBlank(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Check Supabase Version")
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

        // SQL Schema Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Required SQL Schema",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
CREATE TABLE app_versions (
  id SERIAL PRIMARY KEY,
  version TEXT NOT NULL,
  update_type TEXT DEFAULT 'FLEXIBLE',
  release_notes TEXT,
  download_url TEXT,
  platform TEXT DEFAULT 'all',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE app_versions
  ENABLE ROW LEVEL SECURITY;

-- Allow public read access
CREATE POLICY "Public read"
  ON app_versions FOR SELECT
  USING (true);
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}
