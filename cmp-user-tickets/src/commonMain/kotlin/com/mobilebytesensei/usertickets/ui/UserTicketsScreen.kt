package com.mobilebytesensei.usertickets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilebytesensei.usertickets.model.TicketStatus
import com.mobilebytesensei.usertickets.model.TicketType
import com.mobilebytesensei.usertickets.model.UserTicket
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTicketsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: TicketsTab = TicketsTab.REQUESTED,
    initialTicketType: TicketType? = null,
    viewModel: UserTicketsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (initialTicketType != null && !state.showSubmitForm) {
        viewModel.showSubmitForm(initialTicketType)
        viewModel.selectTab(
            if (initialTicketType == TicketType.CONTACT_SUPPORT) {
                TicketsTab.MY_TICKETS
            } else {
                TicketsTab.REQUESTED
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tickets") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showSubmitForm() }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create ticket",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
            ) {
                TicketsTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.selectedTab) {
                    TicketsTab.REQUESTED -> TicketList(
                        tickets = state.publicTickets,
                        emptyMessage = "No feature requests or bug reports yet.\nBe the first to submit one!",
                        showUpvote = true,
                        onTicketClick = viewModel::selectTicket,
                        onUpvote = viewModel::upvoteTicket,
                    )

                    TicketsTab.IMPLEMENTED -> TicketList(
                        tickets = state.resolvedTickets,
                        emptyMessage = "No implemented features yet.\nStay tuned!",
                        showUpvote = false,
                        onTicketClick = viewModel::selectTicket,
                        onUpvote = {},
                    )

                    TicketsTab.MY_TICKETS -> TicketList(
                        tickets = state.myTickets,
                        emptyMessage = "No support tickets.\nNeed help? Tap + to create one.",
                        showUpvote = false,
                        onTicketClick = viewModel::selectTicket,
                        onUpvote = {},
                    )
                }
            }
        }

        if (state.showSubmitForm) {
            CreateTicketDialog(
                ticketType = state.submitFormType,
                isSubmitting = state.isSubmitting,
                onDismiss = viewModel::dismissSubmitForm,
                onSubmit = viewModel::submitTicket,
            )
        }

        state.selectedTicket?.let { ticket ->
            TicketDetailDialog(
                ticket = ticket,
                onDismiss = viewModel::dismissTicketDetail,
                onUpvote = { viewModel.upvoteTicket(ticket.id) },
            )
        }

        state.error?.let {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = viewModel::dismissError) {
                        Text("OK")
                    }
                },
            ) { Text("Failed to submit ticket") }
        }
    }
}

private const val DEFAULT_TICKET_EMOJI = "\uD83D\uDCDD"
internal const val GENERAL_CATEGORY = "general"


@Composable
private fun TicketList(
    tickets: List<UserTicket>,
    emptyMessage: String,
    showUpvote: Boolean,
    onTicketClick: (UserTicket) -> Unit,
    onUpvote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tickets.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tickets, key = { it.id }) { ticket ->
                TicketCard(
                    ticket = ticket,
                    showUpvote = showUpvote,
                    onClick = { onTicketClick(ticket) },
                    onUpvote = { onUpvote(ticket.id) },
                )
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: UserTicket,
    showUpvote: Boolean,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typeEnum = TicketType.entries.find { it.value == ticket.ticketType }

    OutlinedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${typeEnum?.emoji ?: DEFAULT_TICKET_EMOJI} ${ticket.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showUpvote) {
                    TextButton(
                        onClick = onUpvote,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${ticket.upvotes}")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(ticket.status)
                if (ticket.category != GENERAL_CATEGORY) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                ticket.category.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
                if (ticket.adminResponse != null) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                "Responded",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val statusEnum = TicketStatus.entries.find { it.value == status }
    val containerColor = when (status) {
        TicketStatus.COMPLETED.value, TicketStatus.RESOLVED.value ->
            colorScheme.primaryContainer

        TicketStatus.PLANNED.value, TicketStatus.IN_PROGRESS.value ->
            colorScheme.secondaryContainer

        TicketStatus.IN_REVIEW.value ->
            colorScheme.tertiaryContainer

        else -> colorScheme.surfaceContainerHigh
    }
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                statusEnum?.label ?: status,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
        ),
    )
}
