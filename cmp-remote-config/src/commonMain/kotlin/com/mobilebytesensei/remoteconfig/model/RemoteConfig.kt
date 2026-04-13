package com.mobilebytesensei.remoteconfig.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfig(
    val id: String = "",
    @SerialName("product_type") val productType: String = "",
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

enum class ActionType(val value: String) {
    NONE("none"),
    URL("url"),
    DEEPLINK("deeplink"),
    STORE("store"),
    DISMISS("dismiss"),
    ;

    companion object {
        fun from(value: String): ActionType = entries.find { it.value == value } ?: NONE
    }
}
