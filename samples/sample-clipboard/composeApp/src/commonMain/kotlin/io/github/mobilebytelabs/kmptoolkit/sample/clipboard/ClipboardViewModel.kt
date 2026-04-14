package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardContentType
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardFilter
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardHistoryEntry
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardManager
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardManagerConfig
import com.mobilebytelabs.kmptoolkit.clipboard.UrlDetection
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State ────────────────────────────────────────────────────────────────

data class ClipboardUiState(
    val currentContent: String? = null,
    val history: List<ClipboardHistoryEntry> = emptyList(),
    val historyMaxSize: Int = 30,
    val monitorState: ClipboardMonitorState = ClipboardMonitorState.Idle,
    val urlDetections: List<String> = emptyList(),
    val latestChange: ClipboardChange? = null,
    val statusMessage: String = "",
    val isActive: Boolean = false,
    val hasClipboardAccess: Boolean = false,
    val hasNotificationPermission: Boolean = false,
)

// ── Actions (MVI Intent) ────────────────────────────────────────────────────

sealed class ClipboardAction {
    data class Copy(val text: String) : ClipboardAction()
    data class CopyAsync(val text: String) : ClipboardAction()
    data object PasteAsync : ClipboardAction()
    data class CopyFromHistory(val entry: ClipboardHistoryEntry) : ClipboardAction()
    data class RemoveFromHistory(val entry: ClipboardHistoryEntry) : ClipboardAction()
    data object ClearHistory : ClipboardAction()
    data object Pause : ClipboardAction()
    data object Resume : ClipboardAction()
    data object RequestNotificationPermission : ClipboardAction()
}

// ── ViewModel ───────────────────────────────────────────────────────────────

class ClipboardViewModel : ViewModel() {

    private val clipboard = ClipboardManager(
        ClipboardManagerConfig(
            observe = true,
            historySize = 30,
            detectUrls = true,
            urlMatchers = SocialMediaUrlMatchers.all(),
            async = true,
        )
    )

    private val _uiState = MutableStateFlow(ClipboardUiState(
        historyMaxSize = clipboard.historyMaxSize,
        hasClipboardAccess = clipboard.hasClipboardAccess(),
        hasNotificationPermission = clipboard.hasNotificationPermission(),
    ))
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

    init {
        clipboard.start()
        _uiState.update { it.copy(isActive = true) }

        // Observe clipboard content
        viewModelScope.launch {
            clipboard.content.collect { content ->
                _uiState.update { it.copy(currentContent = content) }
            }
        }

        // Observe history
        viewModelScope.launch {
            clipboard.history.collect { entries ->
                _uiState.update { it.copy(history = entries) }
            }
        }

        // Observe monitor state
        viewModelScope.launch {
            clipboard.monitorState.collect { state ->
                _uiState.update { it.copy(monitorState = state) }
            }
        }

        // Observe latest change
        viewModelScope.launch {
            clipboard.latestChange.collect { change ->
                _uiState.update { it.copy(latestChange = change) }
            }
        }

        // Observe URL detections
        viewModelScope.launch {
            clipboard.urlDetections.collect { detection ->
                _uiState.update { state ->
                    val entry = "${detection.matcher.name}: ${detection.url.take(50)}"
                    state.copy(urlDetections = (listOf(entry) + state.urlDetections).take(10))
                }
            }
        }
    }

    fun onAction(action: ClipboardAction) {
        when (action) {
            is ClipboardAction.Copy -> {
                val success = clipboard.copy(action.text)
                _uiState.update { it.copy(
                    statusMessage = if (success) "Copied: ${action.text.take(30)}" else "Copy failed"
                )}
            }

            is ClipboardAction.CopyAsync -> viewModelScope.launch {
                val success = clipboard.copyAsync(action.text)
                _uiState.update { it.copy(
                    statusMessage = if (success) "Async copied: ${action.text.take(30)}" else "Failed"
                )}
            }

            is ClipboardAction.PasteAsync -> viewModelScope.launch {
                val text = clipboard.pasteAsync()
                _uiState.update { it.copy(
                    statusMessage = "Read: ${text?.take(40) ?: "(empty)"}"
                )}
            }

            is ClipboardAction.CopyFromHistory -> {
                clipboard.copyFromHistory(action.entry)
                _uiState.update { it.copy(statusMessage = "Copied from history") }
            }

            is ClipboardAction.RemoveFromHistory -> {
                clipboard.removeFromHistory(action.entry)
            }

            is ClipboardAction.ClearHistory -> {
                clipboard.clearHistory()
                _uiState.update { it.copy(statusMessage = "History cleared") }
            }

            is ClipboardAction.Pause -> {
                clipboard.pause()
                _uiState.update { it.copy(statusMessage = "Paused") }
            }

            is ClipboardAction.Resume -> {
                clipboard.resume()
                _uiState.update { it.copy(statusMessage = "Resumed") }
            }

            is ClipboardAction.RequestNotificationPermission -> viewModelScope.launch {
                val granted = clipboard.requestNotificationPermission()
                _uiState.update { it.copy(
                    hasNotificationPermission = granted,
                    statusMessage = if (granted) "Permission granted" else "Permission denied"
                )}
            }
        }
    }

    override fun onCleared() {
        clipboard.stop()
    }
}
