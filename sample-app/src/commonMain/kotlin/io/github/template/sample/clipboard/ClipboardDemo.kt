package io.github.template.sample.clipboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.template.sample.clipboard.tabs.CopyTab
import io.github.template.sample.clipboard.tabs.PlatformSupportTab
import io.github.template.sample.clipboard.tabs.ReadTab

/**
 * Clipboard Demo - Demonstrates all clipboard functionality.
 *
 * This demo shows how to use the KMP Toolkit clipboard module:
 * - Copy text to clipboard (all platforms)
 * - Read from clipboard (where supported)
 * - Check clipboard status
 * - Platform-specific behaviors
 *
 * Usage in your app:
 * ```kotlin
 * import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
 * import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
 * import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText
 * import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard
 *
 * // Copy text
 * val success = copyToClipboard("Hello, World!")
 *
 * // Read text (where supported)
 * val text = getFromClipboard()
 *
 * // Check if has text
 * val hasText = hasClipboardText()
 *
 * // Clear clipboard
 * clearClipboard()
 * ```
 */
@Composable
fun ClipboardDemo() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Copy", "Read", "Platform Support")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTabIndex) {
            0 -> CopyTab()
            1 -> ReadTab()
            2 -> PlatformSupportTab()
        }
    }
}
