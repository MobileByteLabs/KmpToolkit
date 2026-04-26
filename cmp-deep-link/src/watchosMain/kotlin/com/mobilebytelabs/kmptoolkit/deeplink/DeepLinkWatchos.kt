@file:Suppress("ktlint:standard:no-empty-file")

package com.mobilebytelabs.kmptoolkit.deeplink

// watchOS does not support URL scheme handling or Universal Links.
// There is no WatchKit API for receiving custom URI schemes from the OS.
//
// DeepLinkHandler.handle() remains callable from shared code, but no
// automatic OS delivery occurs on this platform.
//
// This file is intentionally a no-op. See docs/IOS.md for platform notes.
