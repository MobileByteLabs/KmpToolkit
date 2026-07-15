/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */

package com.mobilebytelabs.kmptoolkit.pdfgenerator

/**
 * Default file name used when a caller does not supply one.
 */
internal const val DEFAULT_PDF_FILE_NAME: String = "document.pdf"

/**
 * Normalizes a caller-supplied [fileName] into a safe PDF file name that always carries a
 * single `.pdf` extension.
 *
 * Rules:
 *  - Leading/trailing whitespace is trimmed.
 *  - Blank / whitespace-only input falls back to `document`.
 *  - An existing `.pdf` extension (any case) is preserved, never doubled.
 *  - Any other value gets `.pdf` appended.
 *
 * This is the single source of truth so the file name is identical across every platform's
 * output path — Android (FileProvider/Print), iOS & macOS (temp file + share/save pickers),
 * JVM (save dialog), and JS (download / Web Share). wasmJs accepts the name for API parity but
 * the browser controls the print/save file name, so it is ignored there.
 */
internal fun ensurePdfFileName(fileName: String): String {
    val base = fileName.trim().ifBlank { "document" }
    return if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
}
