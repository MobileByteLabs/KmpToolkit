/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers the pure per-platform selection seam, idempotency, and graceful
 * degradation. Runs on JVM (the Measurement-Protocol tier), where
 * `platformInitializeFirebase` is a no-op — so `FirebaseKit.initialize(config)`
 * exercises the real control flow without touching a native Firebase SDK.
 */
class FirebaseConfigTest {

    private val cfg = FirebaseConfig(
        android = FirebaseOptions(applicationId = "1:1:android:a", apiKey = "k-android"),
        apple = FirebaseOptions(applicationId = "1:1:ios:b", apiKey = "k-apple", gcmSenderId = "1"),
        web = FirebaseOptions(applicationId = "1:1:web:c", apiKey = "k-web"),
    )

    @Test
    fun android_web_and_apple_group_select_correctly() {
        assertSame(cfg.android, cfg.optionsForPlatform("android"))
        assertSame(cfg.web, cfg.optionsForPlatform("js"))
        // Apple is ONE grouped entry for ios/macos/tvos.
        assertSame(cfg.apple, cfg.optionsForPlatform("ios"))
        assertSame(cfg.apple, cfg.optionsForPlatform("macos"))
        assertSame(cfg.apple, cfg.optionsForPlatform("tvos"))
    }

    @Test
    fun fallback_tier_platforms_have_no_native_options() {
        listOf("jvm", "linux", "mingw", "wasmjs", "watchos").forEach {
            assertNull(cfg.optionsForPlatform(it), "expected null native options for $it")
        }
    }

    @Test
    fun missing_keys_degrade_gracefully_without_throwing() {
        val empty = FirebaseConfig()
        assertNull(empty.optionsForCurrentPlatform())
        // No native options + no throw — the graceful-degradation contract.
        platformInitializeFirebase(empty.optionsForCurrentPlatform())
        FirebaseKit.initialize(empty)
        assertTrue(FirebaseKit.isInitialized)
    }

    @Test
    fun initialize_is_idempotent() {
        FirebaseKit.initialize(cfg)
        assertTrue(FirebaseKit.isInitialized)
        // Second call is a safe no-op — never throws, state stays initialized.
        FirebaseKit.initialize(cfg)
        assertTrue(FirebaseKit.isInitialized)
    }

    @Test
    fun builder_produces_equivalent_config() {
        val built = FirebaseConfig.builder()
            .android(cfg.android!!)
            .apple(cfg.apple!!)
            .web(cfg.web!!)
            .build()
        assertNotNull(built.optionsForPlatform("android"))
        assertSame(cfg.apple, built.optionsForPlatform("ios"))
    }
}
