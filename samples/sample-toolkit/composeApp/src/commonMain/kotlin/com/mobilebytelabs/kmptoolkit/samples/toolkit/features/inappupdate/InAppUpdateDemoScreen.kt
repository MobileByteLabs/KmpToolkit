/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.inappupdate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.SetupRequiredCard

@Composable
fun InAppUpdateDemoScreen(onStatus: (String) -> Unit) {
    LaunchedEffect(Unit) { onStatus("Resolver config required") }

    DemoIntro("Update-version intelligence. Resolvers fetch latest-version metadata from Supabase or GitHub Releases.")

    SetupRequiredCard(
        title = "Configure a resolver",
        explanation = "Pick a backend (Supabase / GitHub) and wire it once at app launch. The library then exposes update prompts as Compose composables.",
        setupSteps = listOf(
            "Add SupabaseResolver(url, anonKey, table=\"app_versions\") OR GitHubResolver(owner, repo)",
            "Inject into UpdateChecker, call check() at app start",
            "Render UpdatePrompt() to surface a Material 3 dialog when newer version available",
        ),
    )
}
