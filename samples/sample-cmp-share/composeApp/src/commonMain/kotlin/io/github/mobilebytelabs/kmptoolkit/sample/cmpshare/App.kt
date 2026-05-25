/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Screen {
    Home,
    ShareBasic,
    ShareAdvanced,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            var currentScreen by remember { mutableStateOf(Screen.Home) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when (currentScreen) {
                                    Screen.Home -> "cmp-share Sample"
                                    Screen.ShareBasic -> "Basic Share"
                                    Screen.ShareAdvanced -> "Advanced Share"
                                },
                            )
                        },
                        navigationIcon = {
                            if (currentScreen != Screen.Home) {
                                IconButton(onClick = { currentScreen = Screen.Home }) {
                                    Text("←", fontSize = 20.sp)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                },
            ) { padding ->
                AnimatedContent(
                    targetState = currentScreen,
                    modifier = Modifier.padding(padding),
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(onNavigate = { currentScreen = it })
                        Screen.ShareBasic -> ShareTextScreen()
                        Screen.ShareAdvanced -> ShareAdvancedScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "KMP Toolkit · cmp-share",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Running on ${getPlatform().name}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        DemoCard(
            title = "Basic Share",
            subtitle = "Text · URL · Image · File — exercises Share.text/url/image/file",
            onClick = { onNavigate(Screen.ShareBasic) },
        )
        Spacer(Modifier.height(12.dp))
        DemoCard(
            title = "Advanced Share",
            subtitle = "Multi-payload + ShareOptions (subject, chooserTitle)",
            onClick = { onNavigate(Screen.ShareAdvanced) },
        )

        Spacer(Modifier.height(32.dp))
        Text(
            text = "Each demo dispatches the platform-native share sheet (Android Intent.ACTION_SEND · iOS UIActivityViewController · macOS NSSharingServicePicker · JVM clipboard+FileDialog fallback · Web navigator.share+clipboard fallback).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DemoCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("Open")
            }
        }
    }
}
