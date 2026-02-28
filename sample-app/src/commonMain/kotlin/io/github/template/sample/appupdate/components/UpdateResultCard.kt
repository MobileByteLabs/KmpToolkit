package io.github.template.sample.appupdate.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateResult

/**
 * Displays the result of an update check.
 */
@Composable
fun UpdateResultCard(result: UpdateResult) {
    val (containerColor, title, icon) = when (result) {
        is UpdateResult.Success -> Triple(
            if (result.updateInfo.isAvailable) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            if (result.updateInfo.isAvailable) "Update Available" else "Up to Date",
            if (result.updateInfo.isAvailable) "⬆️" else "✅",
        )

        is UpdateResult.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Error",
            "❌",
        )

        is UpdateResult.NotSupported -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Not Supported",
            "ℹ️",
        )

        is UpdateResult.Cancelled -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Cancelled",
            "🚫",
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = icon, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (result) {
                is UpdateResult.Success -> {
                    val info = result.updateInfo
                    Text("Current: ${info.currentVersion.versionName}")
                    info.latestVersion?.let { latest ->
                        Text("Latest: ${latest.versionName}")
                    }
                    Text("Update Type: ${info.updateType.name}")
                }

                is UpdateResult.Error -> {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                is UpdateResult.NotSupported -> {
                    Text(text = result.reason)
                }

                is UpdateResult.Cancelled -> {
                    Text(text = "Update was cancelled by the user")
                }
            }
        }
    }
}
