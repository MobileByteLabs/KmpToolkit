package com.mobilebytesensei.usertickets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilebytesensei.usertickets.data.UserTicketsRepository
import com.mobilebytesensei.usertickets.model.TicketType
import com.mobilebytesensei.usertickets.model.UserTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserTicketsState(
    val selectedTab: TicketsTab = TicketsTab.REQUESTED,
    val publicTickets: List<UserTicket> = emptyList(),
    val resolvedTickets: List<UserTicket> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

enum class TicketsTab(val label: String) {
    REQUESTED(UserTicketsStrings.TAB_REQUESTED),
    IMPLEMENTED(UserTicketsStrings.TAB_IMPLEMENTED),
}

class UserTicketsViewModel(
    private val repository: UserTicketsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UserTicketsState())
    val state: StateFlow<UserTicketsState> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(tab: TicketsTab) {
        _state.update { it.copy(selectedTab = tab) }
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
        email: String?,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result = repository.submitTicket(
                ticketType = type,
                title = title,
                description = description,
                category = category,
                email = email,
            )
            if (result != null) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = if (type == TicketType.CONTACT_SUPPORT) {
                            UserTicketsStrings.SUPPORT_SUCCESS
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
                        error = UserTicketsStrings.ERROR_SUBMIT_FAILED,
                    )
                }
            }
        }
    }

    fun upvoteTicket(ticketId: String) {
        _state.update { state ->
            state.copy(
                publicTickets = state.publicTickets.map {
                    if (it.id == ticketId) it.copy(upvotes = it.upvotes + 1) else it
                },
            )
        }
        viewModelScope.launch {
            repository.upvoteTicket(ticketId)
        }
    }

    fun getTicketById(ticketId: String): UserTicket? {
        val state = _state.value
        return state.publicTickets.find { it.id == ticketId }
            ?: state.resolvedTickets.find { it.id == ticketId }
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val public = repository.getPublicTickets()
            val resolved = repository.getResolvedTickets()
            _state.update {
                it.copy(
                    publicTickets = public,
                    resolvedTickets = resolved,
                    isLoading = false,
                )
            }
        }
    }
}

internal object UserTicketsStrings {
    const val SCREEN_TITLE = "Tickets"
    const val TAB_REQUESTED = "Requested"
    const val TAB_IMPLEMENTED = "Implemented"
    const val EMPTY_REQUESTED = "No feature requests or bug reports yet.\nBe the first to submit one!"
    const val EMPTY_IMPLEMENTED = "No implemented features yet.\nStay tuned!"
    const val CHIP_RESPONDED = "Responded"
    const val CREATE_TITLE = "Create Ticket"
    const val CREATE_TYPE = "Type"
    const val CREATE_CATEGORY = "Category"
    const val CREATE_FIELD_TITLE = "Title"
    const val CREATE_FIELD_DESCRIPTION = "Description"
    const val CREATE_FIELD_EMAIL_REQUIRED = "Email (required)"
    const val CREATE_FIELD_EMAIL_OPTIONAL = "Email (optional)"
    const val CREATE_ERROR_TITLE = "Title is required"
    const val CREATE_ERROR_DESCRIPTION = "Description is required"
    const val CREATE_ERROR_EMAIL = "Email is required for support tickets"
    const val CREATE_SUBMIT = "Submit Ticket"
    const val SUPPORT_PRIVACY = "This ticket is private and won't be visible in the app. We'll get back to you via email."
    const val SUPPORT_SUCCESS = "Ticket submitted! We'll get back to you via email."
    const val DETAIL_ADMIN_RESPONSE = "Admin Response"
    const val DETAIL_RESOLUTION = "Resolution"
    const val ERROR_SUBMIT_FAILED = "Failed to submit ticket"
    const val OK = "OK"
}
