package com.mobilebytelabs.producttickets.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilebytelabs.producttickets.domain.model.TicketPriority
import com.mobilebytelabs.producttickets.domain.model.TicketStatus
import com.mobilebytelabs.producttickets.domain.model.TicketType
import com.mobilebytelabs.producttickets.domain.model.UserTicket
import org.koin.compose.viewmodel.koinViewModel

private const val DEFAULT_TICKET_EMOJI = "\uD83D\uDCDD"
internal const val GENERAL_CATEGORY = "general"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTicketsScreen(
    onBackClick: () -> Unit,
    onNavigateToCreateTicket: (TicketType) -> Unit,
    onNavigateToTicketDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductTicketsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ProductTicketsStrings.SCREEN_TITLE) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCreateTicket(TicketType.FEATURE_REQUEST) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create ticket",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                edgePadding = 16.dp,
            ) {
                TicketsTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                placeholder = { Text(ProductTicketsStrings.SEARCH_PLACEHOLDER) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.selectedTab) {
                    TicketsTab.REQUESTED -> TicketList(
                        tickets = viewModel.getFilteredTickets(state.publicTickets),
                        emptyMessage = ProductTicketsStrings.EMPTY_REQUESTED,
                        showUpvote = true,
                        onTicketClick = { ticket -> onNavigateToTicketDetail(ticket.id) },
                        onUpvote = viewModel::upvoteTicket,
                    )

                    TicketsTab.ROADMAP -> TicketList(
                        tickets = viewModel.getFilteredTickets(state.roadmapTickets),
                        emptyMessage = ProductTicketsStrings.EMPTY_ROADMAP,
                        showUpvote = true,
                        onTicketClick = { ticket -> onNavigateToTicketDetail(ticket.id) },
                        onUpvote = viewModel::upvoteTicket,
                    )

                    TicketsTab.IMPLEMENTED -> TicketList(
                        tickets = viewModel.getFilteredTickets(state.resolvedTickets),
                        emptyMessage = ProductTicketsStrings.EMPTY_IMPLEMENTED,
                        showUpvote = false,
                        onTicketClick = { ticket -> onNavigateToTicketDetail(ticket.id) },
                        onUpvote = {},
                    )

                    TicketsTab.MY_TICKETS -> TicketList(
                        tickets = viewModel.getFilteredTickets(state.myTickets),
                        emptyMessage = ProductTicketsStrings.EMPTY_MY_TICKETS,
                        showUpvote = false,
                        onTicketClick = { ticket -> onNavigateToTicketDetail(ticket.id) },
                        onUpvote = {},
                    )
                }
            }
        }
    }
}

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
                val priorityEnum = TicketPriority.entries.find { it.value == ticket.priority }
                if (priorityEnum != null && priorityEnum != TicketPriority.MEDIUM) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                "${priorityEnum.emoji} ${priorityEnum.label}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
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
                                ProductTicketsStrings.CHIP_RESPONDED,
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
    val labelColor = when (status) {
        TicketStatus.COMPLETED.value, TicketStatus.RESOLVED.value ->
            colorScheme.onPrimaryContainer

        TicketStatus.PLANNED.value, TicketStatus.IN_PROGRESS.value ->
            colorScheme.onSecondaryContainer

        TicketStatus.IN_REVIEW.value ->
            colorScheme.onTertiaryContainer

        else -> colorScheme.onSurface
    }
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                statusEnum?.label ?: status,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
        ),
    )
}
