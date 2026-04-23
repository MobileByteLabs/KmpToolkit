package com.mobilebytelabs.producttickets.data.remote

import com.mobilebytelabs.producttickets.config.ProductTicketsConfig
import com.mobilebytelabs.producttickets.domain.model.TicketComment
import com.mobilebytelabs.producttickets.domain.model.TicketType
import com.mobilebytelabs.producttickets.domain.model.UserTicket
import com.mobilebytelabs.producttickets.domain.model.UserTicketInsert

class ProductTicketsRepository internal constructor(private val service: ProductTicketsService) {
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
        platform: String? = null,
        appVersion: String? = null,
        severity: String? = null,
        email: String? = null,
        deviceInfo: String? = null,
    ): UserTicket? {
        val insert = UserTicketInsert(
            boardType = ProductTicketsConfig.boardType,
            ticketType = ticketType.value,
            title = title,
            description = description,
            category = category,
            priority = priority,
            platform = platform,
            appVersion = appVersion,
            severity = severity,
            isPrivate = ticketType.isPrivate,
            userId = if (ticketType.isPrivate) ProductTicketsConfig.userId else null,
            userEmail = email,
            deviceInfo = deviceInfo,
        )
        return service.submitTicket(insert)
    }

    suspend fun toggleVote(ticketId: String, voterId: String): Int? = service.toggleVote(ticketId, voterId)

    suspend fun upvoteTicket(ticketId: String) = service.upvoteTicket(ticketId)
}
