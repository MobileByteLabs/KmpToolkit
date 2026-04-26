package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Scheme", "Route", "Builder", "Stream")

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("cmp-deep-link demo") })
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { idx, label ->
                        NavigationBarItem(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            label = { Text(label) },
                            icon = {},
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (selectedTab) {
                    0 -> SchemeHandlerDemo()
                    1 -> RouteParserDemo()
                    2 -> LinkBuilderDemo()
                    3 -> HandlerFlowDemo()
                }
            }
        }
    }
}
