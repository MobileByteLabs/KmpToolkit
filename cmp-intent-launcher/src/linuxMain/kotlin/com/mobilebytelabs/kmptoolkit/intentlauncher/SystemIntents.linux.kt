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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system

/**
 * Linux (K/N) `SystemIntents` actual.
 *
 * - `openAppSettings()` — best-effort via `system()` chain.
 * - `createDocument()` — `zenity --file-selection --save` subprocess; consumes the
 *   chosen path from `zenity`'s stdout. Same zenity dep as the existing
 *   `IntentLauncher` Linux impl (documented in module README).
 */
@OptIn(ExperimentalForeignApi::class)
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult {
        val rc = system(
            "gnome-control-center 2>/dev/null & " +
                "if ! pgrep -x gnome-control-center >/dev/null; then " +
                "kcmshell5 2>/dev/null & " +
                "if ! pgrep -x kcmshell5 >/dev/null; then " +
                "xdg-open settings:// 2>/dev/null & fi; fi",
        )
        return if (rc == 0) {
            IntentResult.Ok(IntentData(uri = "settings://"))
        } else {
            IntentResult.Failed(IntentError.Unknown("system() exit=$rc"))
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult {
        val cmd = "zenity --file-selection --save --confirm-overwrite " +
            "--filename='${suggestedName.shellEscape()}' 2>/dev/null"
        val pipe = popen(cmd, "r") ?: return IntentResult.Failed(IntentError.Unknown("popen failed"))
        val picked = memScoped {
            val buf = allocArray<kotlinx.cinterop.ByteVar>(4096)
            fgets(buf, 4096, pipe)?.toKString()?.trim()
        }
        // pclose returns the wait4 status (signal + exit-code packed); we can't cleanly
        // distinguish "zenity exited with code 1 (user cancelled)" from "zenity not
        // installed (shell exit 127)" without WEXITSTATUS. Both produce empty stdout,
        // so treat empty output as Cancelled — degrades gracefully when zenity is
        // missing (user sees no dialog + no error, equivalent to UX abort).
        pclose(pipe)
        return if (picked.isNullOrEmpty()) {
            IntentResult.Cancelled
        } else {
            IntentResult.Ok(IntentData(uri = "file://$picked", mimeType = mimeType))
        }
    }
}

private fun String.shellEscape(): String = replace("'", "'\\''")
