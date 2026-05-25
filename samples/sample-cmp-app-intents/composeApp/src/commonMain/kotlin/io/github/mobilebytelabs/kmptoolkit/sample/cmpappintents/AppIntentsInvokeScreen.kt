/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi::class)

package io.github.mobilebytelabs.kmptoolkit.sample.cmpappintents

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
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.ParamType
import com.mobilebytelabs.kmptoolkit.appintents.appIntents
import kotlinx.coroutines.launch

@Composable
fun AppIntentsInvokeScreen() {
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Invoke Intent — test mode",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Text(
                text = "TEST MODE: invokeForTesting() calls the perform lambda directly without going through Siri or Spotlight. Real Siri/Spotlight integration ships in v0.2 — see plan D5/D6/D7/D8 in the inter-app-comms-suite.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
            )
        }

        Text(
            text = "Ensure intents are registered before invoking. Navigate to Register Intents first if you haven't already.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                scope.launch {
                    // Ensure the intent is registered for this screen too
                    val config = appIntents {
                        intent("openTransfer") {
                            title = "Open Transfer"
                            description = "Navigate to the money transfer screen"
                            parameter("amount", ParamType.Number, isRequired = false)
                            perform { params ->
                                val amount = params["amount"]
                                AppIntentResult.Dialog("Transfer screen opened — amount=${amount ?: "unspecified"}")
                            }
                        }
                    }
                    AppIntents.register(config)

                    val result = AppIntents.invokeForTesting(
                        "openTransfer",
                        mapOf("amount" to 100),
                    )
                    lastResult = formatInvokeResult(result)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Invoke 'openTransfer' (amount=100)")
        }

        Button(
            onClick = {
                scope.launch {
                    val result = AppIntents.invokeForTesting(
                        "nonExistentIntent",
                        emptyMap(),
                    )
                    lastResult = formatInvokeResult(result)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Invoke unknown intent (expect null)")
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
    }
}

private fun formatInvokeResult(result: AppIntentResult?): String = when (result) {
    null -> "null — intent ID not found in registry (not registered)"
    is AppIntentResult.Dialog -> "Dialog: \"${result.message}\""
    is AppIntentResult.Snippet -> "Snippet (markdown): \"${result.markdown}\""
    AppIntentResult.Done -> "Done (no UI response)"
    is AppIntentResult.Failed -> "Failed: \"${result.message}\""
}
