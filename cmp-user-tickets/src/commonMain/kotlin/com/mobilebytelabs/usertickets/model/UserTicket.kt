package com.mobilebytelabs.usertickets.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserTicket(
    val id: String = "",
    @SerialName("product_type") val productType: String = "",
    @SerialName("ticket_type") val ticketType: String = "feature_request",
    val title: String = "",
    val description: String = "",
    val category: String = "general",
    val status: String = "pending",
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("device_info") val deviceInfo: String? = null,
    val resolution: String? = null,
    @SerialName("admin_response") val adminResponse: String? = null,
    @SerialName("responded_at") val respondedAt: String? = null,
    val upvotes: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UserTicketInsert(
    @SerialName("product_type") val productType: String,
    @SerialName("ticket_type") val ticketType: String,
    val title: String,
    val description: String,
    val category: String = "general",
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("device_info") val deviceInfo: String? = null,
)
