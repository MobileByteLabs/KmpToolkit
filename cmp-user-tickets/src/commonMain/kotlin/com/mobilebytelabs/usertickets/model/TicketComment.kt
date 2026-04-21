package com.mobilebytelabs.usertickets.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TicketComment(
    val id: String = "",
    @SerialName("ticket_id") val ticketId: String = "",
    @SerialName("author_type") val authorType: String = "user",
    @SerialName("author_name") val authorName: String = "Anonymous",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)
