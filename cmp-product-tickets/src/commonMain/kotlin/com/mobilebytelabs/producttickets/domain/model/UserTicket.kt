package com.mobilebytelabs.producttickets.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UserTicket(
    val id: String = "",
    @SerialName("ticket_type") val ticketType: String = "feature_request",
    val title: String = "",
    val description: String = "",
    val category: String = "general",
    val status: String = "pending",
    val priority: String = "medium",
    val platform: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    val milestone: String? = null,
    val labels: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("device_info") val deviceInfo: String? = null,
    val upvotes: Int = 0,
    @SerialName("admin_response") val adminResponse: String? = null,
    @SerialName("responded_at") val respondedAt: String? = null,
    val severity: String? = null,
    val resolution: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // ── Multi-board + Analysis (schema v2.0.0 — migrations 006–009) ──────────
    @SerialName("board_type") val boardType: String = "",
    @SerialName("ticket_analysis") val ticketAnalysis: JsonObject? = null,
    @SerialName("claimed_by") val claimedBy: String? = null,
    @SerialName("claimed_at") val claimedAt: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
)

@Serializable
data class UserTicketInsert(
    @SerialName("board_type") val boardType: String, // mandatory — every ticket belongs to a board
    @SerialName("ticket_type") val ticketType: String,
    val title: String,
    val description: String,
    val category: String = "general",
    val priority: String = "medium",
    val platform: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    val severity: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("device_info") val deviceInfo: String? = null,
    @SerialName("parent_id") val parentId: String? = null, // nullable — set for child tickets
)
