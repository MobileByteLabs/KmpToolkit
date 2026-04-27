@file:Suppress("ktlint:standard:no-empty-file")

package com.mobilebytelabs.kmptoolkit.deeplink

// wasmWasi targets server-side WASI environments (Node.js WASI, wasmtime, etc.)
// There is no browser window object or URL-scheme OS delivery mechanism.
//
// DeepLinkHandler.handle() is still callable from shared code if the host
// environment passes a URI string directly, but no automatic detection exists.
//
// For browser-side routing in Wasm, use wasmJsMain (DeepLinkHandler.initBrowser()).
//
// This file is intentionally a no-op. See docs/WEB.md for platform notes.
