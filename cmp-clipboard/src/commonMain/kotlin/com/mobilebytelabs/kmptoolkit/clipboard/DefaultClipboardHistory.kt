package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Default implementation of [ClipboardHistory].
 *
 * Uses [ClipboardObserver] to detect clipboard changes and maintains
 * an in-memory history list capped at [maxSize].
 */
internal class DefaultClipboardHistory(override val maxSize: Int) : ClipboardHistory {

    private val _entries = MutableStateFlow<List<ClipboardHistoryEntry>>(emptyList())
    override val entries: StateFlow<List<ClipboardHistoryEntry>> = _entries.asStateFlow()

    private var _isCapturing = false
    override val isCapturing: Boolean get() = _isCapturing

    private val observer = createClipboardObserver()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    override fun startCapturing() {
        if (_isCapturing) return
        _isCapturing = true

        observer.startObserving()

        collectJob = scope.launch {
            observer.clipboardContent.collect { content ->
                if (content != null) {
                    addEntry(content)
                }
            }
        }
    }

    override fun stopCapturing() {
        if (!_isCapturing) return
        _isCapturing = false

        collectJob?.cancel()
        collectJob = null
        observer.stopObserving()
    }

    override fun copyToClipboard(entry: ClipboardHistoryEntry): Boolean = copyToClipboard(entry.content)

    override fun remove(entry: ClipboardHistoryEntry) {
        _entries.value = _entries.value.filter { it !== entry && it.timestamp != entry.timestamp }
    }

    override fun clear() {
        _entries.value = emptyList()
    }

    private fun addEntry(content: String) {
        val current = _entries.value

        // Skip if same as most recent entry (dedup)
        if (current.isNotEmpty() && current.first().content == content) return

        val contentType = when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardContentType.Uri
            content.startsWith("<html") || content.contains("</") -> ClipboardContentType.Html
            else -> ClipboardContentType.Text
        }

        val entry = ClipboardHistoryEntry(
            content = content,
            timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            contentType = contentType,
        )

        // Prepend new entry, cap at maxSize
        val updated = listOf(entry) + current
        _entries.value = if (updated.size > maxSize) updated.take(maxSize) else updated
    }
}
