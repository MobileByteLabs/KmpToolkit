package com.mobilebytelabs.remoteconfig.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilebytelabs.remoteconfig.dispatch.ActionDispatcher
import com.mobilebytelabs.remoteconfig.dynamic.DynamicUiRenderer
import com.mobilebytelabs.remoteconfig.dynamic.model.UiAction
import com.mobilebytelabs.remoteconfig.dynamic.parser.UiNodeParser
import com.mobilebytelabs.remoteconfig.model.ActionType
import com.mobilebytelabs.remoteconfig.model.DisplayType
import com.mobilebytelabs.remoteconfig.model.RemoteConfig
import org.koin.compose.viewmodel.koinViewModel

/**
 * Render the active remote-config CTA.
 *
 * Action routing:
 * - Pass [onAction] for explicit per-screen control (escape hatch).
 * - Omit [onAction] and use the `action(...)` DSL inside `remoteConfig { … }` —
 *   actions dispatch to your registered handlers via [ActionDispatcher].
 */
@Composable
fun RemoteConfigHost(
    viewModel: RemoteConfigViewModel = koinViewModel(),
    onAction: ((actionType: ActionType, actionValue: String?) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.activeConfig ?: return

    LaunchedEffect(config.id) {
        viewModel.onConfigShown(config.id)
    }

    val handle: (ActionType, String?) -> Unit = { type, value ->
        if (onAction != null) {
            onAction(type, value)
        } else {
            ActionDispatcher.dispatch(type, value)
        }
    }

    // Route: dynamic (content_json) or static (templates)
    if (config.contentJson != null) {
        DynamicConfigRenderer(
            config = config,
            onAction = { uiAction ->
                val actionType = ActionType(uiAction.type)
                if (actionType == ActionType.DISMISS) {
                    viewModel.onConfigDismissed(config.id)
                } else {
                    handle(actionType, uiAction.value)
                    viewModel.onActionClicked(config.id)
                }
            },
            onDismiss = { viewModel.onConfigDismissed(config.id) },
        )
    } else {
        StaticConfigRenderer(
            config = config,
            onAction = handle,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun DynamicConfigRenderer(config: RemoteConfig, onAction: (UiAction) -> Unit, onDismiss: () -> Unit) {
    val rootNode = UiNodeParser.parse(config.contentJson ?: return) ?: return

    when (DisplayType.from(config.displayType)) {
        DisplayType.DIALOG -> {
            Dialog(
                onDismissRequest = { if (config.isDismissible) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = config.isDismissible,
                    dismissOnClickOutside = config.isDismissible,
                    usePlatformDefaultWidth = false,
                ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                ) {
                    DynamicUiRenderer(rootNode, onAction)
                }
            }
        }

        DisplayType.FULLSCREEN -> {
            Dialog(
                onDismissRequest = { if (config.isDismissible) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = config.isDismissible,
                    usePlatformDefaultWidth = false,
                ),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DynamicUiRenderer(rootNode, onAction)
                }
            }
        }

        DisplayType.BANNER -> {
            DynamicUiRenderer(rootNode, onAction)
        }

        DisplayType.BOTTOM_SHEET -> {
            Dialog(
                onDismissRequest = { if (config.isDismissible) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = config.isDismissible,
                    usePlatformDefaultWidth = false,
                ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                ) {
                    DynamicUiRenderer(rootNode, onAction)
                }
            }
        }
    }
}

@Composable
private fun StaticConfigRenderer(
    config: RemoteConfig,
    onAction: (actionType: ActionType, actionValue: String?) -> Unit,
    viewModel: RemoteConfigViewModel,
) {
    val handlePrimaryAction: () -> Unit = {
        onAction(ActionType(config.actionType), config.actionValue)
        viewModel.onActionClicked(config.id)
    }

    val handleSecondaryAction: () -> Unit = {
        val type = ActionType(config.secondaryActionType)
        if (type == ActionType.DISMISS) {
            viewModel.onConfigDismissed(config.id)
        } else {
            onAction(type, config.secondaryActionValue)
            viewModel.onActionClicked(config.id)
        }
    }

    val handleDismiss: () -> Unit = {
        viewModel.onConfigDismissed(config.id)
    }

    when (DisplayType.from(config.displayType)) {
        DisplayType.DIALOG -> RemoteConfigDialog(
            config = config,
            onPrimaryAction = handlePrimaryAction,
            onSecondaryAction = handleSecondaryAction,
            onDismiss = handleDismiss,
        )

        DisplayType.FULLSCREEN -> RemoteConfigFullScreen(
            config = config,
            onPrimaryAction = handlePrimaryAction,
            onSecondaryAction = handleSecondaryAction,
            onDismiss = handleDismiss,
        )

        DisplayType.BANNER -> RemoteConfigBanner(
            config = config,
            onPrimaryAction = handlePrimaryAction,
            onDismiss = handleDismiss,
        )

        DisplayType.BOTTOM_SHEET -> RemoteConfigBottomSheet(
            config = config,
            onPrimaryAction = handlePrimaryAction,
            onSecondaryAction = handleSecondaryAction,
            onDismiss = handleDismiss,
        )
    }
}
