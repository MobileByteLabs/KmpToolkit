package com.mobilebytelabs.producttickets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilebytelabs.producttickets.config.ProductTicketsConfig
import com.mobilebytelabs.producttickets.data.remote.ProductTicketsRepository
import com.mobilebytelabs.producttickets.domain.model.TicketType
import com.mobilebytelabs.producttickets.domain.model.UserTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductTicketsState(
    val selectedTab: TicketsTab = TicketsTab.REQUESTED,
    val publicTickets: List<UserTicket> = emptyList(),
    val resolvedTickets: List<UserTicket> = emptyList(),
    val roadmapTickets: List<UserTicket> = emptyList(),
    val myTickets: List<UserTicket> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

enum class TicketsTab(val label: String) {
    REQUESTED(ProductTicketsStrings.TAB_REQUESTED),
    ROADMAP(ProductTicketsStrings.TAB_ROADMAP),
    IMPLEMENTED(ProductTicketsStrings.TAB_IMPLEMENTED),
    MY_TICKETS(ProductTicketsStrings.TAB_MY_TICKETS),
}

class ProductTicketsViewModel(private val repository: ProductTicketsRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProductTicketsState())
    val state: StateFlow<ProductTicketsState> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(tab: TicketsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun getFilteredTickets(tickets: List<UserTicket>): List<UserTicket> {
        val query = _state.value.searchQuery.trim().lowercase()
        if (query.isEmpty()) return tickets
        return tickets.filter {
            it.title.lowercase().contains(query) ||
                it.description.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun dismissSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    fun submitTicket(
        type: TicketType,
        title: String,
        description: String,
        category: String,
        priority: String,
        email: String?,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result = repository.submitTicket(
                ticketType = type,
                title = title,
                description = description,
                category = category,
                priority = priority,
                email = email,
            )
            if (result != null) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = if (type == TicketType.CONTACT_SUPPORT) {
                            ProductTicketsStrings.SUPPORT_SUCCESS
                        } else {
                            null
                        },
                    )
                }
                loadAll()
            } else {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = ProductTicketsStrings.ERROR_SUBMIT_FAILED,
                    )
                }
            }
        }
    }

    fun upvoteTicket(ticketId: String) {
        val voterId = ProductTicketsConfig.userId ?: "anon-${ticketId.take(8)}"
        // Optimistic update
        _state.update { state ->
            state.copy(
                publicTickets = state.publicTickets.map {
                    if (it.id == ticketId) it.copy(upvotes = it.upvotes + 1) else it
                },
            )
        }
        viewModelScope.launch {
            val newCount = repository.toggleVote(ticketId, voterId)
            if (newCount != null) {
                _state.update { state ->
                    state.copy(
                        publicTickets = state.publicTickets.map {
                            if (it.id == ticketId) it.copy(upvotes = newCount) else it
                        },
                        roadmapTickets = state.roadmapTickets.map {
                            if (it.id == ticketId) it.copy(upvotes = newCount) else it
                        },
                    )
                }
            }
        }
    }

    fun getTicketById(ticketId: String): UserTicket? {
        val state = _state.value
        return state.publicTickets.find { it.id == ticketId }
            ?: state.resolvedTickets.find { it.id == ticketId }
            ?: state.myTickets.find { it.id == ticketId }
    }

    fun fetchTicketById(ticketId: String, onResult: (UserTicket?) -> Unit) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            onResult(ticket)
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val public = repository.getPublicTickets()
            val resolved = repository.getResolvedTickets()
            val my = repository.getMyTickets()
            val roadmap = public.filter { it.status == "planned" || it.status == "in_progress" }
            _state.update {
                it.copy(
                    publicTickets = public,
                    resolvedTickets = resolved,
                    roadmapTickets = roadmap,
                    myTickets = my,
                    isLoading = false,
                )
            }
        }
    }
}

internal object ProductTicketsStrings {
    const val SCREEN_TITLE = "Tickets"
    const val TAB_REQUESTED = "Requested"
    const val TAB_IMPLEMENTED = "Implemented"
    const val TAB_MY_TICKETS = "My Tickets"
    const val TAB_ROADMAP = "Roadmap"
    const val EMPTY_REQUESTED = "No feature requests or bug reports yet.\nBe the first to submit one!"
    const val EMPTY_IMPLEMENTED = "No implemented features yet.\nStay tuned!"
    const val EMPTY_MY_TICKETS = "No private tickets yet.\nSubmit a support request to see it here."
    const val EMPTY_ROADMAP = "No planned or in-progress items yet.\nStay tuned for what's coming!"
    const val SEARCH_PLACEHOLDER = "Search tickets..."
    const val CHIP_RESPONDED = "Responded"
    const val CREATE_TITLE = "Create Ticket"
    const val CREATE_TYPE = "Type"
    const val CREATE_CATEGORY = "Category"
    const val CREATE_PRIORITY = "Priority"
    const val CREATE_FIELD_TITLE = "Title"
    const val CREATE_FIELD_DESCRIPTION = "Description"
    const val CREATE_FIELD_EMAIL_REQUIRED = "Email (required)"
    const val CREATE_FIELD_EMAIL_OPTIONAL = "Email (optional)"
    const val CREATE_ERROR_TITLE = "Title is required"
    const val CREATE_ERROR_DESCRIPTION = "Description is required"
    const val CREATE_ERROR_EMAIL = "Email is required for support tickets"
    const val CREATE_SUBMIT = "Submit Ticket"
    const val SUPPORT_PRIVACY =
        "This ticket is private and won't be visible in the app. " +
            "We'll get back to you via email."
    const val SUPPORT_SUCCESS = "Ticket submitted! We'll get back to you via email."
    const val DETAIL_ADMIN_RESPONSE = "Admin Response"
    const val DETAIL_RESOLUTION = "Resolution"
    const val ERROR_SUBMIT_FAILED = "Failed to submit ticket"
    const val OK = "OK"
}
