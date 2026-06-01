/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

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
import com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareResult
import kotlinx.coroutines.launch

/**
 * "Try it on this platform" panel — exercises the real `Share` impl on whatever Compose
 * Multiplatform target is currently running (Android / iOS / JVM / JS / wasmJs) for each
 * payload variant (Text / Url / Image / File) and surfaces the typed [ShareResult] below
 * the button grid.
 *
 * The Image bytes are generated in-memory (a 4-byte PNG magic prefix sequence) — no
 * resource fixture needed. On platforms that actually parse the bytes as an image
 * (Android FileProvider, iOS UIImage), the share-sheet will indicate "invalid image"
 * but the API call itself proves the impl wiring works.
 *
 * Authored 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 04. Per Locked
 * Decision D3 ("DoD = compile + Fake test + sample demonstrates it"), this panel
 * is the manual smoke-test surface for the per-platform `Share` impls.
 */
@OptIn(ExperimentalShareApi::class)
@Composable
fun TryItPanel() {
    val scope = rememberCoroutineScope()
    var lastAction by remember { mutableStateOf("(none)") }
    var lastResult by remember { mutableStateOf<ShareResult?>(null) }

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
            "Each button invokes Share.share() with a different payload. The result is rendered below — Completed / Cancelled / Failed(cause).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Share text"
                scope.launch {
                    lastResult = Share.share(
                        SharePayload.Text("Hello from cmp-share sample on ${getPlatform().name}"),
                        ShareOptions(chooserTitle = "Share text"),
                    )
                }
            },
        ) { Text("Share text") }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Share URL"
                scope.launch {
                    lastResult = Share.share(
                        SharePayload.Url("https://kotlinlang.org"),
                        ShareOptions(chooserTitle = "Share URL"),
                    )
                }
            },
        ) { Text("Share URL (kotlinlang.org)") }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Share image (4-byte placeholder PNG)"
                scope.launch {
                    lastResult = Share.share(
                        SharePayload.Image(
                            bytes = SAMPLE_PNG_PLACEHOLDER,
                            mimeType = "image/png",
                            filename = "sample.png",
                        ),
                        ShareOptions(chooserTitle = "Share image"),
                    )
                }
            },
        ) { Text("Share image (placeholder PNG)") }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                lastAction = "Share file (data: URI)"
                scope.launch {
                    lastResult = Share.share(
                        SharePayload.File(
                            uri = "data:image/png;base64,iVBORw0KGgo=",
                            mimeType = "image/png",
                            filename = "sample.png",
                        ),
                        ShareOptions(chooserTitle = "Share file"),
                    )
                }
            },
        ) { Text("Share file") }

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

/** PNG magic header bytes — enough to exercise the byte-passing path without a real image fixture. */
private val SAMPLE_PNG_PLACEHOLDER: ByteArray = byteArrayOf(
    0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
    0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
)

@OptIn(ExperimentalShareApi::class)
private fun formatResult(r: ShareResult?): String = when (r) {
    null -> "(no call yet)"
    ShareResult.Completed -> "Completed"
    ShareResult.Cancelled -> "Cancelled"
    is ShareResult.Failed -> "Failed(${r.cause::class.simpleName})"
}
