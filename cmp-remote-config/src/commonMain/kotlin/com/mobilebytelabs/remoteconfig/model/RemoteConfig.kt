package com.mobilebytelabs.remoteconfig.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfig(
    val id: String = "",
    val platform: String = "all",
    @SerialName("min_app_version") val minAppVersion: String? = null,
    @SerialName("max_app_version") val maxAppVersion: String? = null,
    val title: String = "",
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("display_type") val displayType: String = "dialog",
    val priority: Int = 0,
    @SerialName("is_dismissible") val isDismissible: Boolean = true,
    @SerialName("action_text") val actionText: String? = null,
    @SerialName("action_type") val actionType: String = "none",
    @SerialName("action_value") val actionValue: String? = null,
    @SerialName("secondary_action_text") val secondaryActionText: String? = null,
    @SerialName("secondary_action_type") val secondaryActionType: String = "dismiss",
    @SerialName("secondary_action_value") val secondaryActionValue: String? = null,
    @SerialName("max_impressions") val maxImpressions: Int = 1,
    @SerialName("cooldown_hours") val cooldownHours: Int = 24,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("icon_emoji") val iconEmoji: String? = null,
    @SerialName("content_json") val contentJson: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

enum class DisplayType(val value: String) {
    DIALOG("dialog"),
    FULLSCREEN("fullscreen"),
    BANNER("banner"),
    BOTTOM_SHEET("bottom_sheet"),
    ;

    companion object {
        fun from(value: String): DisplayType = entries.find { it.value == value } ?: DIALOG
    }
}

@Serializable
data class DeviceImpression(
    @SerialName("config_id") val configId: String = "",
    val impressions: Int = 0,
    val dismissed: Boolean = false,
)

/**
 * Open value-class action_type. Built-in constants live on the companion;
 * consumers extend with their own typed constants (recommended pattern):
 *
 * ```
 * object RemoteActions {
 *     val OPEN_DOWNLOADS = ActionType("open_downloads")
 *     val CLEAR_CACHE    = ActionType("clear_cache")
 * }
 * ```
 *
 * Semantics are convention — the library does NOT auto-handle any built-in.
 * Consumer wires them up via `action(...)` DSL inside `remoteConfig { … }`,
 * or via the `RemoteConfigHost(onAction = …)` escape hatch.
 */
@kotlin.jvm.JvmInline
value class ActionType(val value: String) {
    companion object {
        val NONE = ActionType("none")
        val URL = ActionType("url")
        val DEEPLINK = ActionType("deeplink")
        val STORE = ActionType("store")
        val DISMISS = ActionType("dismiss")
        val PREMIUM = ActionType("premium")
    }
}
