package com.mobilebytesensei.usertickets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilebytesensei.usertickets.model.TicketType
import com.mobilebytesensei.usertickets.model.UserTicket

private const val DEFAULT_TICKET_EMOJI = "\uD83D\uDCDD"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TicketDetailScreen(onBackClick: () -> Unit, ticket: UserTicket, onUpvote: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isPublic = !ticket.isPrivate
    val typeEnum = TicketType.entries.find { it.value == ticket.ticketType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title with emoji
            Text(
                text = "${typeEnum?.emoji ?: DEFAULT_TICKET_EMOJI} ${ticket.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Status, category, upvote row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(ticket.status)
                if (ticket.category != GENERAL_CATEGORY) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(ticket.category.replace("_", " ")) },
                    )
                }
                if (isPublic) {
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = onUpvote) {
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

            HorizontalDivider()

            // Full description
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
            )

            // Admin response section
            if (ticket.adminResponse != null) {
                HorizontalDivider()
                Text(
                    text = UserTicketsStrings.DETAIL_ADMIN_RESPONSE,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = ticket.adminResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                if (ticket.respondedAt != null) {
                    Text(
                        text = "Responded: ${ticket.respondedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Resolution section
            if (ticket.resolution != null) {
                HorizontalDivider()
                Text(
                    text = UserTicketsStrings.DETAIL_RESOLUTION,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                )
                Text(
                    text = ticket.resolution,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                )
            }
        }
    }
}
