/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.toast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.toast.ToastHost
import com.mobilebytelabs.kmptoolkit.toast.ToastStyle
import com.mobilebytelabs.kmptoolkit.toast.rememberToastHostState
import kotlinx.coroutines.launch

@Composable
fun ToastDemoScreen(onStatus: (String) -> Unit) {
    val toastState = rememberToastHostState()
    val scope = rememberCoroutineScope()

    DemoIntro("Material 3-styled toasts via ToastHost. Five styles (DEFAULT/SUCCESS/ERROR/WARNING/INFO) mapped to colors. Default position bottom-center.")

    Button(
        onClick = {
            scope.launch { toastState.showToast("Saved successfully", style = ToastStyle.SUCCESS) }
            onStatus("showToast(SUCCESS)")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Show SUCCESS toast") }

    Button(
        onClick = {
            scope.launch { toastState.showToast("Heads up: storage is 80% full", style = ToastStyle.WARNING) }
            onStatus("showToast(WARNING)")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Show WARNING toast") }

    OutlinedButton(
        onClick = {
            scope.launch { toastState.showToast("Failed to upload file", style = ToastStyle.ERROR) }
            onStatus("showToast(ERROR)")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Show ERROR toast") }

    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        ToastHost(hostState = toastState)
    }
}
