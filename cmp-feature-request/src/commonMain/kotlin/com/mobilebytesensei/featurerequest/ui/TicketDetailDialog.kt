package com.mobilebytesensei.featurerequest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilebytesensei.featurerequest.model.TicketType
import com.mobilebytesensei.featurerequest.model.UserTicket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TicketDetailDialog(
    ticket: UserTicket,
    onDismiss: () -> Unit,
    onUpvote: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isPublic = !ticket.isPrivate

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val typeEnum = TicketType.entries.find { it.value == ticket.ticketType }
                Text(
                    text = "${typeEnum?.emoji ?: UserTicketsStrings.DEFAULT_TICKET_EMOJI} ${ticket.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = TicketDetailStrings.CLOSE_CONTENT_DESCRIPTION,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(ticket.status)
                if (ticket.category != UserTicketsStrings.GENERAL_CATEGORY) {
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

            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
            )

            if (ticket.adminResponse != null) {
                HorizontalDivider()
                Text(
                    text = TicketDetailStrings.ADMIN_RESPONSE_TITLE,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        text = ticket.adminResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                if (ticket.respondedAt != null) {
                    Text(
                        text = "${TicketDetailStrings.RESPONDED_PREFIX}${ticket.respondedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (ticket.resolution != null) {
                HorizontalDivider()
                Text(
                    text = TicketDetailStrings.RESOLUTION_TITLE,
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

private object TicketDetailStrings {
    const val CLOSE_CONTENT_DESCRIPTION = "Close"
    const val ADMIN_RESPONSE_TITLE = "Admin Response"
    const val RESPONDED_PREFIX = "Responded: "
    const val RESOLUTION_TITLE = "Resolution"
}
