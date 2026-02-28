package io.github.template.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.template.Greeting
import io.github.template.sample.appupdate.AppUpdateDemo
import io.github.template.sample.clipboard.ClipboardDemo

/**
 * Main entry point for the KMP Toolkit Sample App.
 *
 * This app demonstrates:
 * - Basic KMP library usage (Greeting)
 * - App Update module features
 *
 * Import this project in Android Studio to run on:
 * - Android (sample-app module)
 * - iOS (iosApp scheme)
 * - Desktop (run desktop:run or desktopMain)
 * - Web (wasmJsBrowserDevelopmentRun)
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            var currentScreen by remember { mutableStateOf(Screen.Home) }

            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onNavigateToAppUpdate = { currentScreen = Screen.AppUpdate },
                    onNavigateToClipboard = { currentScreen = Screen.Clipboard },
                    onNavigateToGreeting = { currentScreen = Screen.Greeting },
                )
                Screen.AppUpdate -> AppUpdateScreen(
                    onBack = { currentScreen = Screen.Home },
                )
                Screen.Clipboard -> ClipboardScreen(
                    onBack = { currentScreen = Screen.Home },
                )
                Screen.Greeting -> GreetingScreen(
                    onBack = { currentScreen = Screen.Home },
                )
            }
        }
    }
}

private enum class Screen {
    Home,
    AppUpdate,
    Clipboard,
    Greeting,
}

@Composable
private fun HomeScreen(
    onNavigateToAppUpdate: () -> Unit,
    onNavigateToClipboard: () -> Unit,
    onNavigateToGreeting: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "KMP Toolkit",
            style = MaterialTheme.typography.headlineLarge,
        )

        Text(
            text = "Sample App",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNavigateToAppUpdate,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("App Update Demo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToClipboard,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("Clipboard Demo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToGreeting,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("Greeting Demo")
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Demos available on all platforms:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Android • iOS • macOS • Windows • Linux • Web",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppUpdateScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp),
        ) {
            Text("← Back")
        }

        AppUpdateDemo()
    }
}

@Composable
private fun ClipboardScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp),
        ) {
            Text("← Back")
        }

        ClipboardDemo()
    }
}

@Composable
private fun GreetingScreen(onBack: () -> Unit) {
    var greetingText by remember { mutableStateOf("Click the button to greet!") }
    val greeting = remember { Greeting() }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp),
        ) {
            Text("← Back")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Greeting Demo",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = greetingText,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                greetingText = greeting.greet()
            }) {
                Text("Get Platform Greeting")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                greetingText = greeting.greet("Developer")
            }) {
                Text("Get Personalized Greeting")
            }
        }
    }
}
