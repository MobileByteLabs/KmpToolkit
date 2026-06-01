/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.ResultContracts
import com.mobilebytelabs.kmptoolkit.intentlauncher.SystemIntents
import com.mobilebytelabs.kmptoolkit.intentlauncher.compose.rememberIntentLauncher
import kotlinx.coroutines.launch

/**
 * "Try it on this platform" panel — exercises the real `IntentLauncher` impl on whatever
 * Compose Multiplatform target is currently running (Android / iOS / JVM / JS / wasmJs)
 * and surfaces the typed [IntentResult] in a visible Text below the button grid.
 *
 * Authored 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 04. Per Locked
 * Decision D3 ("DoD = compile + Fake test + sample demonstrates it"), this panel
 * is the manual smoke-test surface for the per-platform `IntentLauncher` impls.
 */
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun TryItPanel() {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()
    var lastAction by remember { mutableStateOf("(none)") }
    var lastResult by remember { mutableStateOf<IntentResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Try it on ${getPlatform().name}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Each button invokes the production IntentLauncher / SystemIntents API. The result is rendered below — Ok / Cancelled / Failed(cause).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Open URL (kotlinlang.org)"
                scope.launch {
                    // IntentBuilder fields are `internal` — use the public fluent
                    // methods (action(), data(), type(), result(), extra()) instead.
                    lastResult = launcher.launch {
                        action("VIEW")
                        data("https://kotlinlang.org")
                    }
                }
            },
        ) { Text("Open URL (kotlinlang.org)") }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Pick image (PickImage contract)"
                scope.launch {
                    lastResult = launcher.launch {
                        result(ResultContracts.PickImage)
                    }
                }
            },
        ) { Text("Pick image") }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Open app settings"
                scope.launch { lastResult = SystemIntents.openAppSettings() }
            },
        ) { Text("Open app settings") }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Text(
            "Last action: $lastAction",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Result: ${formatResult(lastResult)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalIntentLauncherApi::class)
private fun formatResult(r: IntentResult?): String = when (r) {
    null -> "(no call yet)"
    is IntentResult.Ok -> "Ok(${r.data?.uri ?: "null"})"
    IntentResult.Cancelled -> "Cancelled"
    is IntentResult.Failed -> "Failed(${r.cause::class.simpleName})"
}
