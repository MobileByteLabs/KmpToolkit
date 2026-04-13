package com.mobilebytesensei.usertickets.data

import co.touchlab.kermit.Logger
import com.mobilebytesensei.usertickets.config.FeatureRequestConfig
import com.mobilebytesensei.usertickets.model.UserTicket
import com.mobilebytesensei.usertickets.model.UserTicketInsert
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

private const val TABLE = "user_tickets"
private const val TAG = "UserTicketsService"

internal interface UserTicketsService {
    suspend fun getPublicTickets(): List<UserTicket>
    suspend fun getResolvedTickets(): List<UserTicket>
    suspend fun getMyTickets(): List<UserTicket>
    suspend fun submitTicket(ticket: UserTicketInsert): UserTicket?
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
                    neq("status", "completed")
                    neq("status", "resolved")
                    neq("status", "closed")
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
                    or {
                        eq("status", "completed")
                        eq("status", "resolved")
                    }
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

    override suspend fun submitTicket(ticket: UserTicketInsert): UserTicket? = try {
        client.postgrest[TABLE]
            .insert(ticket)
            .decodeSingle()
    } catch (e: Exception) {
        Logger.e(TAG) { "submitTicket failed: ${e.message}" }
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
