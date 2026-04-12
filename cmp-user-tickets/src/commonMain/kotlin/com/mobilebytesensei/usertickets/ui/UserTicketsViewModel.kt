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
    val myTickets: List<UserTicket> = emptyList(),
    val isLoading: Boolean = true,
    val showSubmitForm: Boolean = false,
    val submitFormType: TicketType = TicketType.FEATURE_REQUEST,
    val isSubmitting: Boolean = false,
    val selectedTicket: UserTicket? = null,
    val error: String? = null,
)

enum class TicketsTab(val label: String) {
    REQUESTED("Requested"),
    IMPLEMENTED("Implemented"),
    MY_TICKETS("Support"),
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

    fun showSubmitForm(type: TicketType = TicketType.FEATURE_REQUEST) {
        _state.update { it.copy(showSubmitForm = true, submitFormType = type) }
    }

    fun dismissSubmitForm() {
        _state.update { it.copy(showSubmitForm = false) }
    }

    fun selectTicket(ticket: UserTicket) {
        _state.update { it.copy(selectedTicket = ticket) }
    }

    fun dismissTicketDetail() {
        _state.update { it.copy(selectedTicket = null) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
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
                _state.update { it.copy(isSubmitting = false, showSubmitForm = false) }
                loadAll()
            } else {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = ERROR_KEY_SUBMIT_FAILED,
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

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val public = repository.getPublicTickets()
            val resolved = repository.getResolvedTickets()
            val myTickets = repository.getMyTickets()
            _state.update {
                it.copy(
                    publicTickets = public,
                    resolvedTickets = resolved,
                    myTickets = myTickets,
                    isLoading = false,
                )
            }
        }
    }

    internal companion object {
        const val ERROR_KEY_SUBMIT_FAILED = "error_submit_failed"
    }
}
