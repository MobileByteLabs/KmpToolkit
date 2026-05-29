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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * JVM Desktop `SystemIntents` actual.
 *
 * - `openAppSettings()` — OS-aware shell dispatch (Windows `ms-settings:`, macOS `open`,
 *   Linux gnome-control-center → kcmshell5 → xdg-open chain).
 * - `createDocument()` — Swing `JFileChooser` in SAVE_DIALOG mode, run on EDT.
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult = withContext(Dispatchers.IO) {
        val os = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        val command: Array<String> = when {
            os.contains("win") -> arrayOf("cmd", "/c", "start", "ms-settings:appsfeatures")
            os.contains("mac") -> arrayOf("open", "x-apple.systempreferences:")
            else -> arrayOf("sh", "-c", "gnome-control-center 2>/dev/null || xdg-open settings:// 2>/dev/null")
        }
        try {
            ProcessBuilder(*command).start()
            IntentResult.Ok(IntentData(uri = command.joinToString(" ")))
        } catch (e: Exception) {
            IntentResult.Failed(IntentError.Unknown(e.message ?: "JVM settings launch failed"))
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        withContext(Dispatchers.IO) {
            // Bail early on headless JVM (CI without a display) — `JFileChooser()`
            // throws HeadlessException at construction time, AWT can't paint a dialog.
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                return@withContext IntentResult.Failed(
                    IntentError.Unknown("JVM is headless — JFileChooser cannot present a save dialog"),
                )
            }
            var picked: File? = null
            var dismissed = false
            try {
                // SwingUtilities.invokeAndWait blocks the calling thread until the EDT
                // returns — must not run on a coroutine dispatcher's reusable thread.
                // Dispatchers.IO is a pool sized for blocking; safe to park there.
                SwingUtilities.invokeAndWait {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Save document"
                        selectedFile = File(suggestedName)
                        fileSelectionMode = JFileChooser.FILES_ONLY
                    }
                    val rc = chooser.showSaveDialog(null)
                    if (rc == JFileChooser.APPROVE_OPTION) {
                        picked = chooser.selectedFile
                    } else {
                        dismissed = true
                    }
                }
            } catch (t: Throwable) {
                // Any AWT/HeadlessException/IO failure → Failed (never re-throw out
                // of the suspend boundary so DI callers + tests always see a typed result).
                return@withContext IntentResult.Failed(
                    IntentError.Unknown(t.message ?: t::class.simpleName ?: "JVM picker error"),
                )
            }
            val sel = picked
            when {
                dismissed -> IntentResult.Cancelled
                sel != null -> IntentResult.Ok(IntentData(uri = sel.toURI().toString(), mimeType = mimeType))
                else -> IntentResult.Failed(IntentError.Unknown("JFileChooser returned APPROVE with null file"))
            }
        }
}
