package io.github.mobilebytelabs.kmptoolkit.sample.openurl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.openurl.AppHint
import com.mobilebytelabs.kmptoolkit.openurl.OpenUrlResult
import com.mobilebytelabs.kmptoolkit.openurl.canOpen
import com.mobilebytelabs.kmptoolkit.openurl.open
import com.mobilebytelabs.kmptoolkit.openurl.openInBrowser
import com.mobilebytelabs.kmptoolkit.openurl.openWith
import kotlinx.coroutines.launch

private data class UrlAction(
    val label: String,
    val emoji: String,
    val description: String,
    val color: Color,
    val action: () -> String,
)

@Composable
fun OpenUrlDemoScreen() {
    var lastResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val actions = remember {
        listOf(
            UrlAction(
                label = "Open in Browser",
                emoji = "\uD83C\uDF10",
                description = "\"https://github.com/MobileByteLabs\".open()",
                color = Color(0xFF1565C0),
            ) {
                val ok = "https://github.com/MobileByteLabs".open()
                if (ok) "open() → Success" else "open() → false"
            },
            UrlAction(
                label = "Force Browser",
                emoji = "\uD83D\uDD17",
                description = "\"https://kotlinlang.org\".openInBrowser()",
                color = Color(0xFF6A1B9A),
            ) {
                val ok = "https://kotlinlang.org".openInBrowser()
                if (ok) "openInBrowser() → Success" else "openInBrowser() → false"
            },
            UrlAction(
                label = "Open Email App",
                emoji = "\uD83D\uDCE7",
                description = "\"mailto:hello@example.com\".openWith(AppHint.EMAIL)",
                color = Color(0xFFBF360C),
            ) {
                resultLabel("EMAIL", "mailto:hello@example.com".openWith(AppHint.EMAIL))
            },
            UrlAction(
                label = "Open Maps",
                emoji = "\uD83D\uDDFA\uFE0F",
                description = "\"geo:37.42,-122.08\".openWith(AppHint.MAPS)",
                color = Color(0xFF2E7D32),
            ) {
                resultLabel("MAPS", "geo:37.4219999,-122.0840575".openWith(AppHint.MAPS))
            },
            UrlAction(
                label = "Open Phone Dialer",
                emoji = "\uD83D\uDCDE",
                description = "\"tel:+15550001234\".openWith(AppHint.PHONE)",
                color = Color(0xFF00695C),
            ) {
                resultLabel("PHONE", "tel:+15550001234".openWith(AppHint.PHONE))
            },
            UrlAction(
                label = "Open SMS App",
                emoji = "\uD83D\uDCAC",
                description = "\"sms:+15550001234\".openWith(AppHint.SMS)",
                color = Color(0xFF4527A0),
            ) {
                resultLabel("SMS", "sms:+15550001234".openWith(AppHint.SMS))
            },
            UrlAction(
                label = "Check canOpen",
                emoji = "\u2705",
                description = "\"https://example.com\".canOpen()",
                color = Color(0xFF37474F),
            ) {
                "\"https://example.com\".canOpen() → ${"https://example.com".canOpen()}"
            },
            UrlAction(
                label = "Custom Deep Link",
                emoji = "\uD83D\uDD17",
                description = "\"myapp://home\".open()",
                color = Color(0xFFE65100),
            ) {
                val ok = "myapp://home".open()
                if (ok) "open() → Success" else "open() → false (no handler — expected)"
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Tap any button to invoke the kmp-open-url API.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        actions.forEach { urlAction ->
            UrlActionCard(
                action = urlAction,
                onClick = {
                    scope.launch {
                        lastResult = urlAction.action()
                    }
                },
            )
        }

        lastResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            ResultCard(result = result, onDismiss = { lastResult = null })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun UrlActionCard(action: UrlAction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.08f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = action.emoji, fontSize = 24.sp)
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = action.color,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = action.color),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Run", color = Color.White)
            }
        }
    }
}

@Composable
private fun ResultCard(result: String, onDismiss: () -> Unit) {
    val isError = result.contains("false") || result.contains("NoHandler") || result.contains("Error")
    val bgColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val textColor = if (isError) Color(0xFFB71C1C) else Color(0xFF1B5E20)

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (isError) "\u26A0\uFE0F Result" else "\u2705 Result",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = textColor,
            )
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Dismiss")
            }
        }
    }
}

private fun resultLabel(hint: String, result: OpenUrlResult): String = when (result) {
    is OpenUrlResult.Success -> "openWithApp($hint) → Success"
    is OpenUrlResult.NoHandler -> "openWithApp($hint) → NoHandler (no app installed)"
    is OpenUrlResult.Error -> "openWithApp($hint) → Error: ${result.message}"
}
