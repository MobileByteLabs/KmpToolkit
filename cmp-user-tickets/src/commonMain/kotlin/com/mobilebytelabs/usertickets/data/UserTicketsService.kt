package com.mobilebytelabs.usertickets.data

import co.touchlab.kermit.Logger
import com.mobilebytelabs.usertickets.config.FeatureRequestConfig
import com.mobilebytelabs.usertickets.model.TicketComment
import com.mobilebytelabs.usertickets.model.UserTicket
import com.mobilebytelabs.usertickets.model.UserTicketInsert
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

private const val TABLE = "user_tickets"
private const val TAG = "UserTicketsService"

internal interface UserTicketsService {
    suspend fun getPublicTickets(): List<UserTicket>
    suspend fun getResolvedTickets(): List<UserTicket>
    suspend fun getMyTickets(): List<UserTicket>
    suspend fun getTicketById(ticketId: String): UserTicket?
    suspend fun getComments(ticketId: String): List<TicketComment>
    suspend fun addComment(ticketId: String, authorType: String, authorName: String, content: String): TicketComment?
    suspend fun submitTicket(ticket: UserTicketInsert): UserTicket?
    suspend fun toggleVote(ticketId: String, voterId: String): Int?
    suspend fun upvoteTicket(ticketId: String)
}

internal class UserTicketsServiceImpl : UserTicketsService {

    private val client get() = FeatureRequestClient.instance
    private val productType get() = FeatureRequestConfig.productType

    override suspend fun getPublicTickets(): List<UserTicket> = try {
        client.postgrest[TABLE]
            .select {
                filter {
                    eq("product_type", productType)
                    eq("is_private", false)
                    and {
                        neq("status", "completed")
                        neq("status", "resolved")
                        neq("status", "implemented")
                        neq("status", "closed")
                    }
                }
                order("upvotes", Order.DESCENDING)
            }
            .decodeList()
    } catch (e: Exception) {
        Logger.e(TAG) { "getPublicTickets failed: ${e.message}" }
        emptyList()
    }

    override suspend fun getResolvedTickets(): List<UserTicket> = try {
        client.postgrest[TABLE]
            .select {
                filter {
                    eq("product_type", productType)
                    eq("is_private", false)
                    isIn("status", listOf("completed", "resolved", "implemented"))
                }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList()
    } catch (e: Exception) {
        Logger.e(TAG) { "getResolvedTickets failed: ${e.message}" }
        emptyList()
    }

    override suspend fun getMyTickets(): List<UserTicket> {
        val userId = FeatureRequestConfig.userId ?: return emptyList()
        return try {
            client.postgrest[TABLE]
                .select {
                    filter {
                        eq("product_type", productType)
                        eq("is_private", true)
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList()
        } catch (e: Exception) {
            Logger.e(TAG) { "getMyTickets failed: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun getTicketById(ticketId: String): UserTicket? = try {
        client.postgrest[TABLE]
            .select {
                filter {
                    eq("id", ticketId)
                }
            }
            .decodeSingleOrNull()
    } catch (e: Exception) {
        Logger.e(TAG) { "getTicketById failed: ${e.message}" }
        null
    }

    override suspend fun getComments(ticketId: String): List<TicketComment> = try {
        client.postgrest["ticket_comments"]
            .select {
                filter { eq("ticket_id", ticketId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()
    } catch (e: Exception) {
        Logger.e(TAG) { "getComments failed: ${e.message}" }
        emptyList()
    }

    override suspend fun addComment(
        ticketId: String,
        authorType: String,
        authorName: String,
        content: String,
    ): TicketComment? = try {
        client.postgrest["ticket_comments"]
            .insert(
                TicketComment(
                    ticketId = ticketId,
                    authorType = authorType,
                    authorName = authorName,
                    content = content,
                ),
            )
            .decodeSingle()
    } catch (e: Exception) {
        Logger.e(TAG) { "addComment failed: ${e.message}" }
        null
    }

    override suspend fun submitTicket(ticket: UserTicketInsert): UserTicket? = try {
        client.postgrest[TABLE]
            .insert(ticket)
            .decodeSingle()
    } catch (e: Exception) {
        Logger.e(TAG) { "submitTicket failed: ${e.message}" }
        null
    }

    override suspend fun toggleVote(ticketId: String, voterId: String): Int? = try {
        client.postgrest.rpc(
            function = "toggle_vote",
            parameters = kotlinx.serialization.json.buildJsonObject {
                put("p_ticket_id", kotlinx.serialization.json.JsonPrimitive(ticketId))
                put("p_voter_id", kotlinx.serialization.json.JsonPrimitive(voterId))
            },
        ).decodeAs<Int>()
    } catch (e: Exception) {
        Logger.e(TAG) { "toggleVote failed: ${e.message}" }
        null
    }

    override suspend fun upvoteTicket(ticketId: String) {
        try {
            client.postgrest.rpc(
                function = "upvote_ticket",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put("ticket_id", kotlinx.serialization.json.JsonPrimitive(ticketId))
                },
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "upvoteTicket failed: ${e.message}" }
        }
    }
}
