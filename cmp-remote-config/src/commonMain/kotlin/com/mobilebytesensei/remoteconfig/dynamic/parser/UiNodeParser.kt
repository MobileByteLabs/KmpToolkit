package com.mobilebytesensei.remoteconfig.dynamic.parser

import co.touchlab.kermit.Logger
import com.mobilebytesensei.remoteconfig.dynamic.model.UiAction
import com.mobilebytesensei.remoteconfig.dynamic.model.UiButtonStyle
import com.mobilebytesensei.remoteconfig.dynamic.model.UiNode
import com.mobilebytesensei.remoteconfig.dynamic.model.UiTextStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "UiNodeParser"

object UiNodeParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(jsonString: String): UiNode? {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val rootNode = root["root"]?.jsonObject ?: return null
            parseNode(rootNode)
        } catch (e: Exception) {
            Logger.e(TAG) { "Failed to parse UI JSON: ${e.message}" }
            null
        }
    }

    private fun parseNode(obj: JsonObject): UiNode? {
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return null
        return when (type) {
            "column" -> parseColumn(obj)

            "row" -> parseRow(obj)

            "box" -> parseBox(obj)

            "text" -> parseText(obj)

            "image" -> parseImage(obj)

            "button" -> parseButton(obj)

            "spacer" -> parseSpacer(obj)

            "divider" -> parseDivider(obj)

            "card" -> parseCard(obj)

            "badge" -> parseBadge(obj)

            "icon" -> parseIcon(obj)

            else -> {
                Logger.w(TAG) { "Unknown node type: $type, skipping" }
                null
            }
        }
    }

    private fun parseChildren(obj: JsonObject): List<UiNode> = obj["children"]?.jsonArray?.mapNotNull {
        parseNode(it.jsonObject)
    } ?: emptyList()

    private fun parseColumn(obj: JsonObject) = UiNode.Column(
        children = parseChildren(obj),
        padding = obj.int("padding"),
        spacing = obj.int("spacing"),
        alignment = obj.str("alignment", "start"),
        background = obj.strOrNull("background"),
    )

    private fun parseRow(obj: JsonObject) = UiNode.Row(
        children = parseChildren(obj),
        padding = obj.int("padding"),
        spacing = obj.int("spacing"),
        alignment = obj.str("alignment", "start"),
    )

    private fun parseBox(obj: JsonObject) = UiNode.Box(
        children = parseChildren(obj),
        padding = obj.int("padding"),
        width = obj.intOrNull("width"),
        height = obj.intOrNull("height"),
        alignment = obj.str("alignment", "center"),
        background = obj.strOrNull("background"),
    )

    private fun parseText(obj: JsonObject) = UiNode.Text(
        content = obj.str("content", ""),
        style = UiTextStyle.from(obj.str("style", "body")),
        color = obj.strOrNull("color"),
        align = obj.str("align", "start"),
        fontWeight = obj.strOrNull("fontWeight"),
        maxLines = obj.intOrNull("maxLines"),
    )

    private fun parseImage(obj: JsonObject) = UiNode.Image(
        url = obj.str("url", ""),
        width = obj.intOrNull("width"),
        height = obj.intOrNull("height"),
        cornerRadius = obj.int("cornerRadius"),
        fit = obj.str("fit", "crop"),
    )

    private fun parseButton(obj: JsonObject) = UiNode.Button(
        text = obj.str("text", ""),
        style = UiButtonStyle.from(obj.str("style", "primary")),
        action = obj["action"]?.jsonObject?.let { actionObj ->
            UiAction(
                type = actionObj.str("type", "none"),
                value = actionObj.strOrNull("value"),
            )
        },
        color = obj.strOrNull("color"),
    )

    private fun parseSpacer(obj: JsonObject) = UiNode.Spacer(
        height = obj.int("height"),
        width = obj.int("width"),
    )

    private fun parseDivider(obj: JsonObject) = UiNode.Divider(
        color = obj.strOrNull("color"),
        thickness = obj.int("thickness", 1),
    )

    private fun parseCard(obj: JsonObject) = UiNode.Card(
        children = parseChildren(obj),
        padding = obj.int("padding", 16),
        cornerRadius = obj.int("cornerRadius", 12),
        elevation = obj.int("elevation", 2),
        background = obj.strOrNull("background"),
    )

    private fun parseBadge(obj: JsonObject) = UiNode.Badge(
        text = obj.str("text", ""),
        color = obj.strOrNull("color"),
        backgroundColor = obj.strOrNull("backgroundColor"),
    )

    private fun parseIcon(obj: JsonObject) = UiNode.Icon(
        emoji = obj.str("emoji", ""),
        size = obj.int("size", 24),
    )

    // Helper extensions
    private fun JsonObject.str(key: String, default: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.strOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String, default: Int = 0): Int = this[key]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
}
