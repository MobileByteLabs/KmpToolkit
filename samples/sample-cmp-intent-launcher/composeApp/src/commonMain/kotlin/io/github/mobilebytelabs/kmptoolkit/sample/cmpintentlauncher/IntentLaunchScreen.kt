/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi::class)

package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentError
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.compose.rememberIntentLauncher
import kotlinx.coroutines.launch

@Composable
fun IntentLaunchScreen() {
    val scope = rememberCoroutineScope()
    val launcher = rememberIntentLauncher()
    var lastResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Intent Launch — ACTION_PICK",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Tapping the button launches ACTION_PICK for image/* on Android. On non-Android platforms the result will be IntentResult.Failed(IntentError.UnsupportedPlatform).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                scope.launch {
                    val result = launcher.launch {
                        action("android.intent.action.PICK")
                        type("image/*")
                    }
                    lastResult = formatResult(result)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Launch ACTION_PICK")
        }

        if (lastResult != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Last result",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = lastResult ?: "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Non-Android platforms: unsupported intent dispatch is planned for D1 (iOS URL schemes), D2 (macOS), D3 (desktop/web open-url fallback) — see the inter-app-comms-suite plan.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatResult(result: IntentResult): String = when (result) {
    is IntentResult.Ok -> "OK — uri=${result.data?.uri ?: "null"}, mimeType=${result.data?.mimeType ?: "null"}"
    IntentResult.Cancelled -> "Cancelled by user"
    is IntentResult.Failed -> "Failed: " + formatError(result.cause)
}

private fun formatError(err: IntentError): String = when (err) {
    IntentError.UnsupportedPlatform -> "Unsupported on this platform — see plan D1/D2/D3 for roadmap"
    IntentError.NoHandler -> "No handler available (no app accepted the intent)"
    IntentError.UserGestureMissing -> "User gesture missing (Web only — call from a click handler)"
    is IntentError.Unknown -> "Unknown — " + err.message
}
