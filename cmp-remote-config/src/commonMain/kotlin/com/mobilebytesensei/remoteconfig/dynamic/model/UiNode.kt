package com.mobilebytesensei.remoteconfig.dynamic.model

sealed class UiNode {
    data class Column(
        val children: List<UiNode> = emptyList(),
        val padding: Int = 0,
        val spacing: Int = 0,
        val alignment: String = "start",
        val background: String? = null,
    ) : UiNode()

    data class Row(
        val children: List<UiNode> = emptyList(),
        val padding: Int = 0,
        val spacing: Int = 0,
        val alignment: String = "start",
    ) : UiNode()

    data class Box(
        val children: List<UiNode> = emptyList(),
        val padding: Int = 0,
        val width: Int? = null,
        val height: Int? = null,
        val alignment: String = "center",
        val background: String? = null,
    ) : UiNode()

    data class Text(
        val content: String,
        val style: UiTextStyle = UiTextStyle.BODY,
        val color: String? = null,
        val align: String = "start",
        val fontWeight: String? = null,
        val maxLines: Int? = null,
    ) : UiNode()

    data class Image(
        val url: String,
        val width: Int? = null,
        val height: Int? = null,
        val cornerRadius: Int = 0,
        val fit: String = "crop",
    ) : UiNode()

    data class Button(
        val text: String,
        val style: UiButtonStyle = UiButtonStyle.PRIMARY,
        val action: UiAction? = null,
        val color: String? = null,
    ) : UiNode()

    data class Spacer(val height: Int = 0, val width: Int = 0) : UiNode()

    data class Divider(val color: String? = null, val thickness: Int = 1) : UiNode()

    data class Card(
        val children: List<UiNode> = emptyList(),
        val padding: Int = 16,
        val cornerRadius: Int = 12,
        val elevation: Int = 2,
        val background: String? = null,
    ) : UiNode()

    data class Badge(val text: String, val color: String? = null, val backgroundColor: String? = null) : UiNode()

    data class Icon(val emoji: String, val size: Int = 24) : UiNode()
}

data class UiAction(val type: String, val value: String? = null)

enum class UiTextStyle(val value: String) {
    HEADLINE("headline"),
    TITLE("title"),
    BODY("body"),
    CAPTION("caption"),
    LABEL("label"),
    ;

    companion object {
        fun from(value: String): UiTextStyle = entries.find { it.value == value } ?: BODY
    }
}

enum class UiButtonStyle(val value: String) {
    PRIMARY("primary"),
    OUTLINED("outlined"),
    TEXT("text"),
    ;

    companion object {
        fun from(value: String): UiButtonStyle = entries.find { it.value == value } ?: PRIMARY
    }
}
