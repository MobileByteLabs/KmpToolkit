package com.mobilebytesensei.usertickets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilebytesensei.usertickets.model.TicketCategory
import com.mobilebytesensei.usertickets.model.TicketType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateTicketDialog(
    ticketType: TicketType,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (TicketType, String, String, String, String?) -> Unit,
) {
    var selectedType by remember { mutableStateOf(ticketType) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            TicketCategory.entries.first { it.applicableTo.contains(ticketType) },
        )
    }
    var titleError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }

    val availableCategories =
        TicketCategory.entries.filter { it.applicableTo.contains(selectedType) }

    LaunchedEffect(selectedType) {
        if (!availableCategories.contains(selectedCategory)) {
            selectedCategory = availableCategories.firstOrNull() ?: TicketCategory.GENERAL
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Create Ticket",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TicketType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text("${type.emoji} ${type.label}") },
                    )
                }
            }

            if (selectedType != TicketType.CONTACT_SUPPORT || availableCategories.size > 1) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    availableCategories.take(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    "${cat.emoji} ${cat.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
                if (availableCategories.size > 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableCategories.drop(4).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        "${cat.emoji} ${cat.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("Title") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required") }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; descriptionError = false },
                label = { Text("Description") },
                isError = descriptionError,
                supportingText = if (descriptionError) {
                    { Text("Description is required") }
                } else {
                    null
                },
                minLines = 4,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = false },
                label = {
                    Text(
                        if (selectedType.isPrivate) {
                            "Email (required)"
                        } else {
                            "Email (optional)"
                        },
                    )
                },
                isError = emailError,
                supportingText = if (emailError) {
                    { Text("Email is required for support tickets") }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    titleError = title.isBlank()
                    descriptionError = description.isBlank()
                    emailError = selectedType.isPrivate && email.isBlank()
                    if (!titleError && !descriptionError && !emailError) {
                        onSubmit(
                            selectedType,
                            title.trim(),
                            description.trim(),
                            selectedCategory.value,
                            email.ifBlank { null },
                        )
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Submit Ticket", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

}
