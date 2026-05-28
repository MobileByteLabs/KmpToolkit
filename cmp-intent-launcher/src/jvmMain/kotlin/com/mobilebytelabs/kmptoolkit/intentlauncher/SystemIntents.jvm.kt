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
            var selected: File? = null
            var dismissed = false
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
                    selected = chooser.selectedFile
                } else {
                    dismissed = true
                }
            }
            when {
                dismissed -> IntentResult.Cancelled
                selected != null -> IntentResult.Ok(
                    IntentData(uri = selected!!.toURI().toString(), mimeType = mimeType),
                )
                else -> IntentResult.Failed(IntentError.Unknown("JFileChooser returned APPROVE with null file"))
            }
        }
}
