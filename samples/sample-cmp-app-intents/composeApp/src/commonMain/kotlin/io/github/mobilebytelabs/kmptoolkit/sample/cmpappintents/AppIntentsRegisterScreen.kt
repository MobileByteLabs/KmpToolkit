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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsConfig
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.ParamType
import com.mobilebytelabs.kmptoolkit.appintents.appIntents

@Composable
fun AppIntentsRegisterScreen() {
    // Build config once and register on first composition
    val config by remember {
        mutableStateOf(
            buildAndRegister(),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Register App Intents",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "On first compose, appIntents { ... } builds the config and AppIntents.register(config) is called. iOS/macOS: writes JSON manifest for Siri Shortcuts. Android: registers on-device App Actions. Other platforms: in-memory only.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Registered intent IDs (${config.intents.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )

        if (config.intents.isEmpty()) {
            Text(
                text = "(none)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    config.intents.forEachIndexed { index, def ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        }
                        Text(
                            text = def.id,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "title: ${def.title}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (def.description.isNotBlank()) {
                            Text(
                                text = "description: ${def.description}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        if (def.parameters.isNotEmpty()) {
                            Text(
                                text = "params: ${def.parameters.joinToString { "${it.name}:${it.type::class.simpleName}" }}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Text(
                            text = "searchable: ${def.searchable}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

private fun buildAndRegister(): AppIntentsConfig {
    val config = appIntents {
        intent("openTransfer") {
            title = "Open Transfer"
            description = "Navigate to the money transfer screen"
            parameter("amount", ParamType.Number, isRequired = false)
            parameter("recipient", ParamType.Text, isRequired = false)
            searchable(category = "Finance")
            perform { params ->
                val amount = params["amount"]
                AppIntentResult.Dialog("Transfer screen opened — amount=${amount ?: "unspecified"}")
            }
        }
        intent("checkBalance") {
            title = "Check Balance"
            description = "Show the current account balance"
            searchable(category = "Finance")
            perform { _ ->
                AppIntentResult.Snippet(markdown = "**Balance**: \$1,234.56")
            }
        }
        intent("findTransaction") {
            title = "Find Transaction"
            description = "Search transactions by keyword"
            parameter("query", ParamType.Text, isRequired = true)
            perform { params ->
                val query = params["query"] as? String ?: ""
                AppIntentResult.Dialog("Searching transactions for: $query")
            }
        }
    }
    AppIntents.register(config)
    return config
}
