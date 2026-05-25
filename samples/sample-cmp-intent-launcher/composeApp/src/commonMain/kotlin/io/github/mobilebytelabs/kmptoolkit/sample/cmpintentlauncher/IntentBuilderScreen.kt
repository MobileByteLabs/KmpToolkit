/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi::class)

package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentError
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.rememberIntentLauncher
import kotlinx.coroutines.launch

@Composable
fun IntentBuilderScreen() {
    val scope = rememberCoroutineScope()
    val launcher = rememberIntentLauncher()

    var actionInput by remember { mutableStateOf("android.intent.action.VIEW") }
    var typeInput by remember { mutableStateOf("text/plain") }
    var dataInput by remember { mutableStateOf("") }
    var addSubjectExtra by remember { mutableStateOf(false) }
    var addTitleExtra by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Intent Builder — custom DSL",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Build a custom intent using the IntentBuilder DSL. Fill in the fields below and tap Launch.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = actionInput,
            onValueChange = { actionInput = it },
            label = { Text("Action") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = typeInput,
            onValueChange = { typeInput = it },
            label = { Text("MIME type") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = dataInput,
            onValueChange = { dataInput = it },
            label = { Text("Data URI (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(
            text = "Preset extras",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = addSubjectExtra, onCheckedChange = { addSubjectExtra = it })
            Text("Add android.intent.extra.SUBJECT = \"KMP Toolkit Demo\"", fontSize = 12.sp)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = addTitleExtra, onCheckedChange = { addTitleExtra = it })
            Text("Add android.intent.extra.TITLE = \"Shared via cmp-intent-launcher\"", fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                scope.launch {
                    val result = launcher.launch {
                        action(actionInput.trim())
                        if (typeInput.isNotBlank()) type(typeInput.trim())
                        if (dataInput.isNotBlank()) data(dataInput.trim())
                        if (addSubjectExtra) extra("android.intent.extra.SUBJECT", "KMP Toolkit Demo")
                        if (addTitleExtra) extra("android.intent.extra.TITLE", "Shared via cmp-intent-launcher")
                    }
                    lastResult = formatBuilderResult(result)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Launch custom Intent")
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

private fun formatBuilderResult(result: IntentResult): String = when (result) {
    is IntentResult.Ok -> "OK — uri=${result.data?.uri ?: "null"}, extras=${result.data?.extras}"
    IntentResult.Cancelled -> "Cancelled by user"
    is IntentResult.Failed -> "Failed: " + when (result.cause) {
        IntentError.UnsupportedPlatform -> "Unsupported on this platform"
        IntentError.NoHandler -> "No handler available"
        IntentError.UserGestureMissing -> "User gesture missing (Web only)"
        is IntentError.Unknown -> "Unknown — " + (result.cause as IntentError.Unknown).message
    }
}
