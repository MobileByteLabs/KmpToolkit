package io.github.template.sample.appupdate.tabs

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.template.sample.appupdate.components.ConfigPreview

/**
 * Config builder playground.
 *
 * Shows:
 * - All platform configuration options
 * - Platform enable/disable toggles
 * - Live code preview
 */
@Composable
fun ConfigBuilderTab() {
    // Platform configs
    var androidPackageName by remember { mutableStateOf("com.example.app") }
    var androidEnabled by remember { mutableStateOf(true) }

    var iosAppStoreId by remember { mutableStateOf("123456789") }
    var iosEnabled by remember { mutableStateOf(true) }

    var macosAppStoreId by remember { mutableStateOf("987654321") }
    var macosEnabled by remember { mutableStateOf(true) }

    var jvmDownloadUrl by remember { mutableStateOf("") }
    var jvmEnabled by remember { mutableStateOf(true) }

    var linuxStoreUrl by remember { mutableStateOf("") }
    var linuxEnabled by remember { mutableStateOf(true) }

    var windowsStoreUrl by remember { mutableStateOf("") }
    var windowsEnabled by remember { mutableStateOf(true) }

    // Resolver config
    var githubOwner by remember { mutableStateOf("") }
    var githubRepo by remember { mutableStateOf("") }

    // Generate code preview
    val codePreview by remember {
        derivedStateOf {
            buildCodePreview(
                androidPackageName = androidPackageName,
                androidEnabled = androidEnabled,
                iosAppStoreId = iosAppStoreId,
                iosEnabled = iosEnabled,
                macosAppStoreId = macosAppStoreId,
                macosEnabled = macosEnabled,
                jvmDownloadUrl = jvmDownloadUrl,
                jvmEnabled = jvmEnabled,
                linuxStoreUrl = linuxStoreUrl,
                linuxEnabled = linuxEnabled,
                windowsStoreUrl = windowsStoreUrl,
                windowsEnabled = windowsEnabled,
                githubOwner = githubOwner,
                githubRepo = githubRepo,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Android Section
        PlatformConfigSection(
            title = "Android",
            enabled = androidEnabled,
            onEnabledChange = { androidEnabled = it },
        ) {
            OutlinedTextField(
                value = androidPackageName,
                onValueChange = { androidPackageName = it },
                label = { Text("Package Name") },
                placeholder = { Text("com.example.app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = androidEnabled,
            )
        }

        // iOS Section
        PlatformConfigSection(
            title = "iOS",
            enabled = iosEnabled,
            onEnabledChange = { iosEnabled = it },
        ) {
            OutlinedTextField(
                value = iosAppStoreId,
                onValueChange = { iosAppStoreId = it },
                label = { Text("App Store ID") },
                placeholder = { Text("123456789") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = iosEnabled,
            )
        }

        // macOS Section
        PlatformConfigSection(
            title = "macOS",
            enabled = macosEnabled,
            onEnabledChange = { macosEnabled = it },
        ) {
            OutlinedTextField(
                value = macosAppStoreId,
                onValueChange = { macosAppStoreId = it },
                label = { Text("Mac App Store ID") },
                placeholder = { Text("987654321") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = macosEnabled,
            )
        }

        // Desktop Platforms
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Desktop Platforms",
                    style = MaterialTheme.typography.titleMedium,
                )

                // JVM
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = jvmEnabled, onCheckedChange = { jvmEnabled = it })
                    Text("JVM Desktop")
                }
                if (jvmEnabled) {
                    OutlinedTextField(
                        value = jvmDownloadUrl,
                        onValueChange = { jvmDownloadUrl = it },
                        label = { Text("Download URL") },
                        placeholder = { Text("https://example.com/app.jar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider()

                // Linux
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = linuxEnabled, onCheckedChange = { linuxEnabled = it })
                    Text("Linux")
                }
                if (linuxEnabled) {
                    OutlinedTextField(
                        value = linuxStoreUrl,
                        onValueChange = { linuxStoreUrl = it },
                        label = { Text("Store URL") },
                        placeholder = { Text("https://flathub.org/apps/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider()

                // Windows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = windowsEnabled, onCheckedChange = { windowsEnabled = it })
                    Text("Windows")
                }
                if (windowsEnabled) {
                    OutlinedTextField(
                        value = windowsStoreUrl,
                        onValueChange = { windowsStoreUrl = it },
                        label = { Text("Store URL") },
                        placeholder = { Text("ms-windows-store://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Resolver Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Version Resolver (GitHub)",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = githubOwner,
                    onValueChange = { githubOwner = it },
                    label = { Text("Owner") },
                    placeholder = { Text("username or organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = githubRepo,
                    onValueChange = { githubRepo = it },
                    label = { Text("Repository") },
                    placeholder = { Text("repository-name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Code Preview
        ConfigPreview(code = codePreview)
    }
}

@Composable
private fun PlatformConfigSection(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enabled, onCheckedChange = onEnabledChange)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (enabled) {
                content()
            }
        }
    }
}

private fun buildCodePreview(
    androidPackageName: String,
    androidEnabled: Boolean,
    iosAppStoreId: String,
    iosEnabled: Boolean,
    macosAppStoreId: String,
    macosEnabled: Boolean,
    jvmDownloadUrl: String,
    jvmEnabled: Boolean,
    linuxStoreUrl: String,
    linuxEnabled: Boolean,
    windowsStoreUrl: String,
    windowsEnabled: Boolean,
    githubOwner: String,
    githubRepo: String,
): String {
    val lines = mutableListOf<String>()
    lines.add("val config = AppUpdateConfig.builder()")

    // Android
    if (androidEnabled && androidPackageName.isNotBlank()) {
        lines.add("    .android(\"$androidPackageName\")")
    } else if (!androidEnabled) {
        lines.add("    .disableAndroid()")
    }

    // iOS
    if (iosEnabled && iosAppStoreId.isNotBlank()) {
        lines.add("    .ios(\"$iosAppStoreId\")")
    } else if (!iosEnabled) {
        lines.add("    .disableIos()")
    }

    // macOS
    if (macosEnabled && macosAppStoreId.isNotBlank()) {
        lines.add("    .macos(\"$macosAppStoreId\")")
    } else if (!macosEnabled) {
        lines.add("    .disableMacos()")
    }

    // JVM
    if (jvmEnabled && jvmDownloadUrl.isNotBlank()) {
        lines.add("    .jvm(downloadUrl = \"$jvmDownloadUrl\")")
    } else if (!jvmEnabled) {
        lines.add("    .disableJvm()")
    }

    // Linux
    if (linuxEnabled && linuxStoreUrl.isNotBlank()) {
        lines.add("    .linux(storeUrl = \"$linuxStoreUrl\")")
    } else if (!linuxEnabled) {
        lines.add("    .disableLinux()")
    }

    // Windows
    if (windowsEnabled && windowsStoreUrl.isNotBlank()) {
        lines.add("    .windows(storeUrl = \"$windowsStoreUrl\")")
    } else if (!windowsEnabled) {
        lines.add("    .disableWindows()")
    }

    // GitHub resolver
    if (githubOwner.isNotBlank() && githubRepo.isNotBlank()) {
        lines.add("    .github(\"$githubOwner\", \"$githubRepo\")")
    }

    lines.add("    .build()")

    return lines.joinToString("\n")
}
