package com.mobilebytelabs.usertickets.data

import com.mobilebytelabs.usertickets.config.FeatureRequestConfig
import com.mobilebytelabs.usertickets.model.TicketComment
import com.mobilebytelabs.usertickets.model.TicketType
import com.mobilebytelabs.usertickets.model.UserTicket
import com.mobilebytelabs.usertickets.model.UserTicketInsert

class UserTicketsRepository internal constructor(private val service: UserTicketsService) {
    suspend fun getPublicTickets(): List<UserTicket> = service.getPublicTickets()

    suspend fun getResolvedTickets(): List<UserTicket> = service.getResolvedTickets()

    suspend fun getMyTickets(): List<UserTicket> = service.getMyTickets()

    suspend fun getTicketById(ticketId: String): UserTicket? = service.getTicketById(ticketId)

    suspend fun getComments(ticketId: String): List<TicketComment> = service.getComments(ticketId)

    suspend fun addComment(ticketId: String, authorName: String, content: String): TicketComment? =
        service.addComment(ticketId, "user", authorName, content)

    suspend fun submitTicket(
        ticketType: TicketType,
        title: String,
        description: String,
        category: String = "general",
        priority: String = "medium",
        email: String? = null,
        deviceInfo: String? = null,
    ): UserTicket? {
        val insert = UserTicketInsert(
            productType = FeatureRequestConfig.productType,
            ticketType = ticketType.value,
            title = title,
            description = description,
            category = category,
            priority = priority,
            isPrivate = ticketType.isPrivate,
            userId = if (ticketType.isPrivate) FeatureRequestConfig.userId else null,
            userEmail = email,
            deviceInfo = deviceInfo,
        )
        return service.submitTicket(insert)
    }

    suspend fun toggleVote(ticketId: String, voterId: String): Int? = service.toggleVote(ticketId, voterId)

    suspend fun upvoteTicket(ticketId: String) = service.upvoteTicket(ticketId)
}
