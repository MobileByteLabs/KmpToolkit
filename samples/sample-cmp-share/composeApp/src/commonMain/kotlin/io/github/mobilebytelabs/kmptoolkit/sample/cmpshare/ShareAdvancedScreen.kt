/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi::class)

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.multi
import kotlinx.coroutines.launch

@Composable
fun ShareAdvancedScreen() {
    val scope = rememberCoroutineScope()
    var textPart by remember { mutableStateOf("Check this out:") }
    var urlPart by remember { mutableStateOf("https://github.com/mobilebytelabs/kmptoolkit") }
    var chooserTitle by remember { mutableStateOf("Share via…") }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Multi-payload share with options",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Sends both a text and a URL in one share, plus ShareOptions(subject, chooserTitle).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = textPart,
            onValueChange = { textPart = it },
            label = { Text("Text part") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = urlPart,
            onValueChange = { urlPart = it },
            label = { Text("URL part") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = chooserTitle,
            onValueChange = { chooserTitle = it },
            label = { Text("Chooser title (Android only)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                scope.launch {
                    val payloads = listOf(
                        SharePayload.Text(textPart),
                        SharePayload.Url(urlPart),
                    )
                    val options = ShareOptions(
                        chooserTitle = chooserTitle.takeIf { it.isNotBlank() },
                    )
                    val result = Share.multi(payloads, options)
                    lastResult = "Result: $result"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Share text + URL together")
        }

        if (lastResult != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = lastResult ?: "",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
