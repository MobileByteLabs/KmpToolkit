/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

/**
 * Lifecycle-free system-level intents — entry points for OS surfaces that round-trip
 * through proxy presenters (Android proxy Activity, iOS root view controller) so the
 * call site doesn't need an `ActivityResultLauncher` / `UIViewController` of its own.
 *
 * Unlike [IntentLauncher], which requires a Composable lifecycle to register
 * `rememberLauncherForActivityResult`, `SystemIntents` methods are callable from
 * commonMain / DI graphs / pure non-UI code. The trade-off: Android consumers must
 * declare the `IntentLauncherInitProvider` (auto-bundled via library AndroidManifest)
 * and the `CreateDocumentProxyActivity` so the OS dispatch + result round-trip can run
 * without a host Activity.
 *
 * Added in v0.4 (inter-app-comms-compose-completeness post-implementation follow-up).
 */
@ExperimentalIntentLauncherApi
public expect object SystemIntents {
    /**
     * Opens the host application's settings screen.
     *
     * | Platform | Behaviour |
     * |---|---|
     * | Android | `ACTION_APPLICATION_DETAILS_SETTINGS` + `package:` URI + `FLAG_ACTIVITY_NEW_TASK` |
     * | iOS | `UIApplication.openURL(URL(UIApplicationOpenSettingsURLString))` |
     * | macOS | `NSWorkspace.open(URL("x-apple.systempreferences:"))` |
     * | JVM Desktop | `ProcessBuilder` OS-specific (`ms-settings:` / `open` / `gnome-control-center`) |
     * | Linux (K/N) | `system("gnome-control-center || kcmshell5 || xdg-open settings://")` |
     * | Windows (K/N) | `system("start ms-settings:appsfeatures")` |
     * | JS / wasmJs | `Failed(UnsupportedPlatform)` — browsers have no app-scoped settings |
     * | tvOS / watchOS | `Failed(UnsupportedPlatform)` — no programmatic settings deep-link |
     */
    public suspend fun openAppSettings(): IntentResult

    /**
     * Opens an OS save dialog for the user to choose a destination + filename, and
     * returns the chosen location's URI in `IntentResult.Ok(IntentData(uri = ...))`.
     *
     * | Platform | Behaviour |
     * |---|---|
     * | Android | Launches the invisible `CreateDocumentProxyActivity` which dispatches `ACTION_CREATE_DOCUMENT` via `ActivityResultContracts.CreateDocument` and signals the captured URI back |
     * | iOS | Presents `UIDocumentPickerViewController` in export mode from the key window's root view controller |
     * | macOS | `NSSavePanel.runModal()` on the main queue |
     * | JVM Desktop | `JFileChooser` SAVE_DIALOG, run on EDT |
     * | JS / wasmJs | `window.showSaveFilePicker()` (File System Access API; falls back to `<a download>` trigger when not supported) |
     * | Linux (K/N) | `zenity --file-selection --save` subprocess |
     * | Windows (K/N) | `Failed(UnsupportedPlatform)` — no save-dialog cinterop yet (use Win32 `GetSaveFileNameW` in a future expansion) |
     * | tvOS / watchOS | `Failed(UnsupportedPlatform)` |
     *
     * Returns `Cancelled` when the user dismisses the picker without choosing,
     * `Failed(UnsupportedPlatform)` on platforms with no save-dialog surface.
     *
     * @param suggestedName Pre-filled filename in the picker. Implementations may strip
     *   the extension and apply [mimeType]'s extension instead, depending on platform UX.
     * @param mimeType MIME type used to filter the picker / suggest extension
     *   (e.g. `"application/pdf"`, `"image/png"`).
     */
    public suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult
}
