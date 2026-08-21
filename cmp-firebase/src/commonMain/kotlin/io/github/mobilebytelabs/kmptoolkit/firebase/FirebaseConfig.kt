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

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.kmpPlatform
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig

/**
 * The single, commonMain Firebase configuration for [FirebaseKit.initialize].
 *
 * One object carries every platform's [FirebaseOptions]. At init, the library
 * selects the entry matching the running platform (via [kmpPlatform]) and
 * initializes Firebase programmatically — no `google-services.json`,
 * `GoogleService-Info.plist`, or Swift `FirebaseApp.configure()` line required.
 *
 * ### Tier routing
 * - **Native (GitLive)** — `android`, `apple` (ios/macos/tvos), `web` (js) →
 *   `Firebase.initialize(options)`.
 * - **Measurement-Protocol** — jvm / watchos / linux / windows / wasmjs have no
 *   native Firebase (or a stub); analytics flows through [measurementProtocol].
 *   Leave [measurementProtocol] null to no-op analytics on those targets.
 *
 * Apple is ONE grouped entry: iOS, macOS and tvOS share [apple]. A platform with
 * no matching options degrades to a no-op init + NoOp analytics (never throws).
 *
 * ### Usage
 * ```kotlin
 * FirebaseKit.initialize(
 *     FirebaseConfig(
 *         android = FirebaseOptions("1:123:android:abc", "AIza…", projectId = "my-proj"),
 *         apple   = FirebaseOptions("1:123:ios:def", "AIza…", gcmSenderId = "123"),
 *         web     = FirebaseOptions("1:123:web:ghi", "AIza…", authDomain = "my-proj.firebaseapp.com"),
 *         measurementProtocol = MpConfig("G-XXXX", apiSecret = secureStore.read("MP_API_SECRET")),
 *     ),
 * )
 * ```
 */
public data class FirebaseConfig(
    val android: FirebaseOptions? = null,
    /** iOS + macOS + tvOS (one grouped entry). */
    val apple: FirebaseOptions? = null,
    /** Web (js). */
    val web: FirebaseOptions? = null,
    /** Measurement-Protocol transport for the non-GitLive tier. */
    val measurementProtocol: MpConfig? = null,
) {
    /**
     * Pure, unit-testable selector: the [FirebaseOptions] for a given [kmpPlatform]
     * string, or `null` when this platform is on the Measurement-Protocol tier
     * (or no options were supplied).
     */
    internal fun optionsForPlatform(platform: String): FirebaseOptions? = when (platform) {
        "android" -> android
        "ios", "macos", "tvos" -> apple
        "js" -> web
        else -> null // jvm / watchos / linux / mingw / wasmjs → MP tier, no native options
    }

    /** The [FirebaseOptions] for the platform this code is running on. */
    public fun optionsForCurrentPlatform(): FirebaseOptions? = optionsForPlatform(kmpPlatform)

    /** Fluent builder — mirrors the toolkit's `AppUpdateConfig` DSL. */
    public class Builder {
        private var android: FirebaseOptions? = null
        private var apple: FirebaseOptions? = null
        private var web: FirebaseOptions? = null
        private var mp: MpConfig? = null

        public fun android(options: FirebaseOptions): Builder = apply { this.android = options }
        public fun apple(options: FirebaseOptions): Builder = apply { this.apple = options }
        public fun web(options: FirebaseOptions): Builder = apply { this.web = options }
        public fun measurementProtocol(config: MpConfig): Builder = apply { this.mp = config }

        public fun build(): FirebaseConfig = FirebaseConfig(android, apple, web, mp)
    }

    public companion object {
        public fun builder(): Builder = Builder()
    }
}
