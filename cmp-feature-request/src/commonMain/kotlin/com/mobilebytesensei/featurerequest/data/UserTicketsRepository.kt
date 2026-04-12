package com.mobilebytesensei.featurerequest.data

import com.mobilebytesensei.featurerequest.config.FeatureRequestConfig
import com.mobilebytesensei.featurerequest.model.TicketType
import com.mobilebytesensei.featurerequest.model.UserTicket
import com.mobilebytesensei.featurerequest.model.UserTicketInsert

class UserTicketsRepository internal constructor(
    private val service: UserTicketsService,
) {
    suspend fun getPublicTickets(): List<UserTicket> = service.getPublicTickets()

    suspend fun getResolvedTickets(): List<UserTicket> = service.getResolvedTickets()

    suspend fun getMyTickets(): List<UserTicket> = service.getMyTickets()

    suspend fun submitTicket(
        ticketType: TicketType,
        title: String,
        description: String,
        category: String = "general",
        email: String? = null,
        deviceInfo: String? = null,
    ): UserTicket? {
        val insert = UserTicketInsert(
            productType = FeatureRequestConfig.productType,
            ticketType = ticketType.value,
            title = title,
            description = description,
            category = category,
            isPrivate = ticketType.isPrivate,
            userId = if (ticketType.isPrivate) FeatureRequestConfig.userId else null,
            userEmail = email,
            deviceInfo = deviceInfo,
        )
        return service.submitTicket(insert)
    }

    suspend fun upvoteTicket(ticketId: String) = service.upvoteTicket(ticketId)
}
