/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.producttickets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.SetupRequiredCard
import com.mobilebytelabs.producttickets.config.ProductTicketsConfig

@Composable
fun ProductTicketsDemoScreen(onStatus: (String) -> Unit) {
    LaunchedEffect(Unit) {
        onStatus("ProductTicketsConfig.supabaseUrl=${ProductTicketsConfig.supabaseUrl.ifEmpty { "(unset)" }}")
    }

    DemoIntro("Self-serve feature-request / bug-report / contact-support flow. Reads + writes to a per-project Supabase `product_tickets` table with RLS. Now ships on JVM (3.3.0).")

    Button(
        onClick = {
            ProductTicketsConfig.init(
                supabaseUrl = "https://demo.supabase.co",
                supabaseAnonKey = "demo-anon-key",
                userId = "catalog-user",
            )
            onStatus("Configured (demo) — url=${ProductTicketsConfig.supabaseUrl}, user=${ProductTicketsConfig.userId}")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Set demo config (no network call)") }

    SetupRequiredCard(
        title = "Real backend required for interactive demo",
        explanation = "Each project owns its own Supabase project (no shared product_type column). Schema: 23 columns including ticket_type, title, description, status, priority — see docs/user-tickets/SETUP.md for the migration SQL.",
        setupSteps = listOf(
            "Create a Supabase project + run migrations/v3.0.0.sql",
            "Call ProductTicketsConfig.init(url, anonKey, userId = null)",
            "Add productTicketsModule to your Koin DI graph",
            "Wire productTicketsDestination / createTicketDestination / ticketDetailDestination into your NavHost",
        ),
    )
}
