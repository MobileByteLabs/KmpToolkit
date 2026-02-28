package io.github.template.sample.appupdate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.template.sample.appupdate.tabs.BasicUpdateTab
import io.github.template.sample.appupdate.tabs.ConfigBuilderTab
import io.github.template.sample.appupdate.tabs.CustomResolverTab
import io.github.template.sample.appupdate.tabs.GitHubResolverTab
import io.github.template.sample.appupdate.tabs.SupabaseResolverTab

/**
 * Main App Update demo screen with tabbed navigation.
 *
 * Demonstrates all App Update features:
 * - Basic update checking
 * - GitHub Releases integration
 * - Supabase backend integration
 * - Custom resolver implementation
 * - Config builder playground
 */
@Composable
fun AppUpdateDemo() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Basic",
        "GitHub",
        "Supabase",
        "Custom",
        "Config",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "App Update Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTabIndex) {
            0 -> BasicUpdateTab()
            1 -> GitHubResolverTab()
            2 -> SupabaseResolverTab()
            3 -> CustomResolverTab()
            4 -> ConfigBuilderTab()
        }
    }
}
