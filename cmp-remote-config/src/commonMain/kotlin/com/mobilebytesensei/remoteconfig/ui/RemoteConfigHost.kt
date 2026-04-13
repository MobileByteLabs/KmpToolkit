package com.mobilebytesensei.remoteconfig.ui

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
import com.mobilebytesensei.remoteconfig.dynamic.DynamicUiRenderer
import com.mobilebytesensei.remoteconfig.dynamic.model.UiAction
import com.mobilebytesensei.remoteconfig.dynamic.parser.UiNodeParser
import com.mobilebytesensei.remoteconfig.model.ActionType
import com.mobilebytesensei.remoteconfig.model.DisplayType
import com.mobilebytesensei.remoteconfig.model.RemoteConfig
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RemoteConfigHost(
    viewModel: RemoteConfigViewModel = koinViewModel(),
    onAction: (actionType: ActionType, actionValue: String?) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.activeConfig ?: return

    LaunchedEffect(config.id) {
        viewModel.onConfigShown(config.id)
    }

    // Route: dynamic (content_json) or static (templates)
    if (config.contentJson != null) {
        DynamicConfigRenderer(
            config = config,
            onAction = { uiAction ->
                val actionType = ActionType.from(uiAction.type)
                if (actionType == ActionType.DISMISS) {
                    viewModel.onConfigDismissed(config.id)
                } else {
                    onAction(actionType, uiAction.value)
                    viewModel.onActionClicked(config.id)
                }
            },
            onDismiss = { viewModel.onConfigDismissed(config.id) },
        )
    } else {
        StaticConfigRenderer(
            config = config,
            onAction = onAction,
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
        onAction(ActionType.from(config.actionType), config.actionValue)
        viewModel.onActionClicked(config.id)
    }

    val handleSecondaryAction: () -> Unit = {
        val type = ActionType.from(config.secondaryActionType)
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
