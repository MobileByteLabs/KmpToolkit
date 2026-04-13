package com.mobilebytelabs.remoteconfig.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobilebytelabs.remoteconfig.model.RemoteConfig

@Composable
internal fun RemoteConfigDialog(
    config: RemoteConfig,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (config.isDismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = config.isDismissible,
            dismissOnClickOutside = config.isDismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                config.iconEmoji?.let {
                    Text(text = it, fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = config.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                config.description?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                config.actionText?.let {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = it, fontWeight = FontWeight.SemiBold)
                    }
                }

                config.secondaryActionText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = it)
                    }
                }

                if (config.isDismissible && config.actionText == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
