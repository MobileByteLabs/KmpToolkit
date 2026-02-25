package com.mobilebytelabs.kmptoolkit.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * JVM Desktop implementation of clipboard operations using AWT Toolkit.
 *
 * This implementation provides full clipboard support on JVM desktop
 * applications using the AWT system clipboard.
 *
 * Note: This requires a graphical environment. Headless JVM environments
 * may not support clipboard operations.
 */

actual fun copyToClipboard(text: String): Boolean {
    return try {
        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun getFromClipboard(): String? {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

actual fun hasClipboardText(): Boolean {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
    } catch (e: Exception) {
        false
    }
}

actual fun clearClipboard() {
    try {
        val selection = StringSelection("")
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    } catch (e: Exception) {
        // Silently ignore errors
    }
}
