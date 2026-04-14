package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.bubble.Bubble
import com.mobilebytelabs.kmptoolkit.bubble.BubbleAction
import com.mobilebytelabs.kmptoolkit.bubble.BubbleConfig
import com.mobilebytelabs.kmptoolkit.bubble.BubbleIcon
import com.mobilebytelabs.kmptoolkit.bubble.BubblePermission
import com.mobilebytelabs.kmptoolkit.bubble.BubbleScreenConfig
import com.mobilebytelabs.kmptoolkit.bubble.BubbleState
import com.mobilebytelabs.kmptoolkit.bubble.BubbleStyle
import com.mobilebytelabs.kmptoolkit.bubble.BubbleTapAction
import com.mobilebytelabs.kmptoolkit.bubble.createBubble
import com.mobilebytelabs.kmptoolkit.bubble.createBubblePermission
import kotlinx.coroutines.launch

@Composable
fun BubbleScreen() {
    val bubble = remember { createBubble(BubbleConfig(sound = true)) }
    val permission = remember { createBubblePermission() }
    val bubbleState by bubble.state.collectAsState()
    val scope = rememberCoroutineScope()
    var lastAction by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Bubble Demo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cross-platform floating UI & notifications",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // State Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (bubbleState) {
                    is BubbleState.Showing -> MaterialTheme.colorScheme.primaryContainer
                    is BubbleState.ActionTaken -> MaterialTheme.colorScheme.tertiaryContainer
                    is BubbleState.Dismissed -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bubble State", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (val s = bubbleState) {
                        is BubbleState.Hidden -> "Hidden"
                        is BubbleState.Showing -> "Showing"
                        is BubbleState.Dismissed -> "Dismissed (by user: ${s.byUser})"
                        is BubbleState.ActionTaken -> "Action: ${s.actionId}"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (lastAction.isNotEmpty()) {
                    Text(
                        text = "Last action: $lastAction",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permissions", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bubble: ${if (permission.canShowBubble()) "Granted" else "Not available"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Notification: ${if (permission.canShowNotification()) "Granted" else "Not granted"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val granted = permission.requestNotificationPermission()
                            lastAction = "Notification permission: $granted"
                        }
                    },
                ) {
                    Text("Request Notification Permission")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Show Variants ---
        Text(
            text = "Show Variants",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 1. Basic notification
        Button(
            onClick = {
                bubble.show(
                    title = "Hello from KMP!",
                    message = "This is a cross-platform bubble notification.",
                )
                lastAction = "show(basic)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Basic Notification")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. With actions
        Button(
            onClick = {
                bubble.show(
                    title = "Download Complete",
                    message = "video.mp4 saved to gallery",
                    actions = listOf(
                        BubbleAction("Open") { lastAction = "Open clicked" },
                        BubbleAction("Share") { lastAction = "Share clicked" },
                        BubbleAction("Delete") { lastAction = "Delete clicked" },
                    ),
                    style = BubbleStyle.Notification,
                )
                lastAction = "show(with actions)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("With Actions (Open/Share/Delete)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. URL detection simulation
        Button(
            onClick = {
                bubble.show(
                    title = "Instagram URL Detected",
                    message = "https://www.instagram.com/reel/ABC123/",
                    icon = BubbleIcon.System("download"),
                    actions = listOf(
                        BubbleAction("Download") { lastAction = "Download started" },
                        BubbleAction("Open in Browser") { lastAction = "Opening browser" },
                    ),
                    onTap = BubbleTapAction.DeepLink("myapp://download?url=instagram"),
                )
                lastAction = "show(URL detection)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("URL Detection (Instagram)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Chat head style
        Button(
            onClick = {
                bubble.show(
                    title = "John",
                    message = "Hey, are you free for a call?",
                    icon = BubbleIcon.System("person"),
                    style = BubbleStyle.Floating,
                    onTap = BubbleTapAction.Callback { lastAction = "Chat opened" },
                )
                lastAction = "show(chat head)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Chat Head Style")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Show screen
        Button(
            onClick = {
                bubble.showScreen(
                    title = "Quick Reply",
                    route = "myapp://chat/reply/456",
                    screenConfig = BubbleScreenConfig(height = 400),
                    icon = BubbleIcon.System("reply"),
                )
                lastAction = "showScreen(route)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Screen (Deep Link)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Persistent (service-like)
        Button(
            onClick = {
                bubble.showPersistent(
                    title = "Clipboard Monitor Active",
                    message = "Monitoring for social media URLs",
                    actions = listOf(
                        BubbleAction("Pause") { lastAction = "Paused" },
                        BubbleAction("Stop") {
                            bubble.dismiss()
                            lastAction = "Stopped"
                        },
                    ),
                    style = BubbleStyle.Service,
                )
                lastAction = "showPersistent(service)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Persistent Service Notification")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 7. Auto-dismiss
        Button(
            onClick = {
                bubble.show(
                    title = "Quick Alert",
                    message = "This will auto-dismiss in 3 seconds",
                    autoDismissMs = 3000L,
                    style = BubbleStyle.Auto,
                )
                lastAction = "show(auto-dismiss 3s)"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Auto-Dismiss (3 seconds)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Update & Dismiss ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(
                onClick = {
                    bubble.update(
                        title = "Updated!",
                        message = "Content changed at runtime",
                    )
                    lastAction = "update()"
                },
            ) {
                Text("Update")
            }

            OutlinedButton(
                onClick = {
                    bubble.dismiss()
                    lastAction = "dismiss()"
                },
            ) {
                Text("Dismiss")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Platform", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getPlatform().name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Bubble support: ${if (permission.canShowBubble()) "Floating" else "Notification"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
