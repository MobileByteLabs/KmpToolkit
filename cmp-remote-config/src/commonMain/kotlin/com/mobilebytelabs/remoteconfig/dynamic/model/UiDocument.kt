package com.mobilebytelabs.remoteconfig.dynamic.model

/**
 * A parsed dynamic-UI document: the tree plus the contract version it was authored against.
 *
 * The version travels WITH the tree because the two are only meaningful together — a host deciding
 * whether to render has to weigh "what did the author target" against "what can this binary do",
 * and that judgement cannot be made from the node tree alone.
 *
 * @property root the parsed tree; null when the payload had no `root` or failed to parse.
 * @property schemaVersion the declared `schema_version`; 1 when absent (pre-versioning trees).
 */
data class UiDocument(val root: UiNode?, val schemaVersion: Int) {
    /** True when the document targets a contract newer than this build understands. */
    val isForwardVersion: Boolean
        get() = schemaVersion > com.mobilebytelabs.remoteconfig.dynamic.parser.UiNodeParser.SUPPORTED_SCHEMA_VERSION
}
