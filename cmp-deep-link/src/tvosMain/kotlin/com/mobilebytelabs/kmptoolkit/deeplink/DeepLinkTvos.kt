@file:Suppress("ktlint:standard:no-empty-file")

package com.mobilebytelabs.kmptoolkit.deeplink

// tvOS does not support URL scheme handling or Universal Links.
// There is no OS-level entry point that delivers a custom URI to a tvOS app.
//
// DeepLinkHandler.handle() is still callable (e.g. from business logic) but
// no automatic OS delivery occurs on this platform.
//
// This file is intentionally a no-op. See docs/IOS.md for platform notes.
