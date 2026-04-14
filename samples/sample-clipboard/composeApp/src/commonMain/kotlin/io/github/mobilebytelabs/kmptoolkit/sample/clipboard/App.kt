package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LibraryItem(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color,
)

enum class Screen {
    Home,
    Clipboard,
    Bubble,
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
                                    Screen.Home -> "KMP Toolkit"
                                    Screen.Clipboard -> "Clipboard"
                                    Screen.Bubble -> "Bubble"
                                }
                            )
                        },
                        navigationIcon = {
                            if (currentScreen != Screen.Home) {
                                IconButton(onClick = { currentScreen = Screen.Home }) {
                                    Text("\u2190", fontSize = 20.sp)
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
                        Screen.Home -> HomeGrid(onItemClick = { currentScreen = it })
                        Screen.Clipboard -> ClipboardDemoTabs()
                        Screen.Bubble -> BubbleScreen()
                    }
                }
            }
        }
    }
}

/**
 * Clipboard module demo with tabs for all clipboard features:
 * Manual, Observer, Monitor, Async
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipboardDemoTabs() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Unified", "Manual", "Observer", "Monitor", "Async")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 -> ClipboardManagerScreen()
            1 -> ClipboardScreen()
            2 -> ClipboardObserverScreen()
            3 -> ClipboardMonitorScreen()
            4 -> ClipboardAsyncScreen()
        }
    }
}

@Composable
private fun HomeGrid(onItemClick: (Screen) -> Unit) {
    val items = listOf(
        LibraryItem(
            title = "Clipboard",
            subtitle = "Copy, paste, observe, monitor, async",
            emoji = "\uD83D\uDCCB",
            color = Color(0xFF4CAF50),
        ) to Screen.Clipboard,
        LibraryItem(
            title = "Bubble",
            subtitle = "Notifications + floating UI",
            emoji = "\uD83D\uDCAC",
            color = Color(0xFFE91E63),
        ) to Screen.Bubble,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Sample Demos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )

        Text(
            text = "Tap a module to see usage examples",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items) { (item, screen) ->
                LibraryCard(
                    item = item,
                    onClick = { onItemClick(screen) },
                )
            }
        }
    }
}

@Composable
private fun LibraryCard(
    item: LibraryItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 40.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = item.color,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
