/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

import java.awt.Frame
import java.awt.Image as AwtImage
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.FileDialog
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM Desktop best-effort share — Java AWT system clipboard for text/url/image,
 * AWT `FileDialog` (SAVE) for file payloads.
 *
 * JVM has no native share-sheet abstraction. Documented as "best-effort" in SPEC.md.
 */
@ExperimentalShareApi
public actual object Share {

    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult =
        withContext(Dispatchers.IO) {
            try {
                when (payload) {
                    is SharePayload.Text -> writeClipboardText(payload.content)
                    is SharePayload.Url -> writeClipboardText(payload.href)
                    is SharePayload.Image -> writeClipboardImage(payload.bytes)
                    is SharePayload.File -> saveFileDialog(payload, options.chooserTitle)
                    is SharePayload.Multi -> shareMulti(payload, options)
                }
            } catch (e: Throwable) {
                ShareResult.Failed(ShareError.Unknown(e.message ?: "JVM share error"))
            }
        }

    private fun systemClipboard(): Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    private fun writeClipboardText(content: String): ShareResult {
        systemClipboard().setContents(StringSelection(content), null)
        return ShareResult.Completed
    }

    private fun writeClipboardImage(bytes: ByteArray): ShareResult {
        if (bytes.isEmpty()) return ShareResult.Failed(ShareError.Unknown("Empty image bytes"))
        val image = ImageIO.read(ByteArrayInputStream(bytes))
            ?: return ShareResult.Failed(ShareError.Unknown("ImageIO could not decode bytes"))
        systemClipboard().setContents(TransferableImage(image), null)
        return ShareResult.Completed
    }

    private fun saveFileDialog(file: SharePayload.File, title: String?): ShareResult {
        val sourcePath = uriToFilePath(file.uri)
            ?: return ShareResult.Failed(
                ShareError.Unknown("JVM Desktop only supports file:// URIs and local paths; got: ${file.uri}")
            )
        val source = File(sourcePath)
        if (!source.exists()) {
            return ShareResult.Failed(ShareError.Unknown("Source file not found: $sourcePath"))
        }

        val dialog = FileDialog(null as Frame?, title ?: "Save", FileDialog.SAVE).apply {
            this.file = file.filename ?: source.name
            isVisible = true
        }
        val chosenDir = dialog.directory ?: return ShareResult.Cancelled
        val chosenName = dialog.file ?: return ShareResult.Cancelled
        val dest = File(chosenDir, chosenName)
        return try {
            source.copyTo(dest, overwrite = true)
            ShareResult.Completed
        } catch (e: IOException) {
            ShareResult.Failed(ShareError.Unknown("Copy failed: ${e.message}"))
        }
    }

    private fun shareMulti(multi: SharePayload.Multi, options: ShareOptions): ShareResult {
        // Strategy: take the first non-Multi item that succeeds (text/url copies to clipboard,
        // file saves via FileDialog). Multi-payload on JVM is a degenerate case — documented.
        if (multi.items.isEmpty()) {
            return ShareResult.Failed(ShareError.Unknown("Multi payload is empty"))
        }
        // Concatenate text/url items into a clipboard write; file items prompt FileDialog separately
        val texts = multi.items.mapNotNull {
            when (it) {
                is SharePayload.Text -> it.content
                is SharePayload.Url -> it.href
                else -> null
            }
        }
        if (texts.isNotEmpty()) {
            writeClipboardText(texts.joinToString("\n"))
        }
        // For the first File item, prompt FileDialog
        val firstFile = multi.items.firstOrNull { it is SharePayload.File } as? SharePayload.File
        if (firstFile != null) {
            return saveFileDialog(firstFile, options.chooserTitle)
        }
        return ShareResult.Completed
    }

    private fun uriToFilePath(uri: String): String? = when {
        uri.startsWith("file://") -> uri.removePrefix("file://")
        uri.startsWith("/") -> uri
        !uri.contains("://") -> uri // bare relative path
        else -> null
    }
}

private class TransferableImage(private val image: AwtImage) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
    override fun getTransferData(flavor: DataFlavor): Any =
        if (flavor == DataFlavor.imageFlavor) image else throw UnsupportedFlavorException(flavor)
}
