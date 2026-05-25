/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit

import androidx.compose.runtime.Composable

/**
 * One [LibraryDemo] per cmp-* library in the toolkit.
 *
 * Adding a new library = append one entry below + create a new demo screen in
 * `features/{id}/` that exports a `@Composable fun {Id}DemoScreen(onStatus: (String) -> Unit)`.
 * No NavHost edits required — [SampleToolkitApp] iterates this list.
 */
data class LibraryDemo(
    val id: String,
    val title: String,
    val tagline: String,
    val category: Category,
    val demo: @Composable (onStatus: (String) -> Unit) -> Unit,
) {
    val route: String get() = "demo/$id"
}

enum class Category(val label: String) {
    UI("UI"),
    Comms("Comms & Sharing"),
    Network("Network"),
    Lifecycle("Lifecycle & Updates"),
    Data("Data & Storage"),
    Backend("Backend"),
}

object LibraryCatalog {
    val all: List<LibraryDemo> = listOf(
        LibraryDemo(
            id = "toast",
            title = "cmp-toast",
            tagline = "Material 3 snackbar/toast utilities",
            category = Category.UI,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.toast.ToastDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "bubble",
            title = "cmp-bubble",
            tagline = "Floating UI bubbles + notifications",
            category = Category.UI,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.bubble.BubbleDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "clipboard",
            title = "cmp-clipboard",
            tagline = "Multiplatform clipboard read / write / observe",
            category = Category.Data,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.clipboard.ClipboardDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "share",
            title = "cmp-share",
            tagline = "Native share sheet — one API, every target",
            category = Category.Comms,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.share.ShareDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "intent-launcher",
            title = "cmp-intent-launcher",
            tagline = "Compose-scoped Intent launcher + ActivityResult",
            category = Category.Comms,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.intentlauncher.IntentLauncherDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "app-intents",
            title = "cmp-app-intents",
            tagline = "Siri Shortcuts / Android App Actions DSL",
            category = Category.Comms,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.appintents.AppIntentsDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "open-url",
            title = "cmp-open-url",
            tagline = "Cross-platform URL / mailto / tel / sms launcher",
            category = Category.Comms,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.openurl.OpenUrlDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "deep-link",
            title = "cmp-deep-link",
            tagline = "Unified deep link handling across all KMP targets",
            category = Category.Comms,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.deeplink.DeepLinkDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "network-monitor",
            title = "cmp-network-monitor",
            tagline = "Reactive connectivity + quality monitoring",
            category = Category.Network,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.networkmonitor.NetworkMonitorDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "in-app-update",
            title = "cmp-in-app-update",
            tagline = "Update prompts + version intelligence",
            category = Category.Lifecycle,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.inappupdate.InAppUpdateDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "pdf-generator",
            title = "cmp-pdf-generator",
            tagline = "HTML / Markdown / DSL → PDF",
            category = Category.Data,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.pdfgenerator.PdfGeneratorDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "remote-config",
            title = "cmp-remote-config",
            tagline = "Firebase Remote Config wrapper",
            category = Category.Backend,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.remoteconfig.RemoteConfigDemoScreen(onStatus)
            },
        ),
        LibraryDemo(
            id = "firebase-analytics",
            title = "cmp-firebase-analytics",
            tagline = "GitLive (11 targets) + Measurement Protocol (10 targets)",
            category = Category.Backend,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.firebaseanalytics.FirebaseAnalyticsDemoScreen(
                    onStatus,
                )
            },
        ),
        LibraryDemo(
            id = "product-tickets",
            title = "cmp-product-tickets",
            tagline = "Feature request / bug report / contact support",
            category = Category.Backend,
            demo = { onStatus ->
                com.mobilebytelabs.kmptoolkit.samples.toolkit.features.producttickets.ProductTicketsDemoScreen(onStatus)
            },
        ),
    )

    fun byId(id: String): LibraryDemo? = all.firstOrNull { it.id == id }
}
