package io.github.template.sample.appupdate.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Displays detailed version information.
 */
@Composable
fun VersionInfoCard(
    version: String,
    releaseNotes: String? = null,
    downloadUrl: String? = null,
    storeUrl: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Version Details",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "Version: $version",
                style = MaterialTheme.typography.bodyLarge,
            )

            releaseNotes?.let { notes ->
                HorizontalDivider()
                Text(
                    text = "Release Notes",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            downloadUrl?.let { url ->
                HorizontalDivider()
                Text(
                    text = "Download URL",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            storeUrl?.let { url ->
                if (downloadUrl == null) {
                    HorizontalDivider()
                }
                Text(
                    text = "Store URL",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
