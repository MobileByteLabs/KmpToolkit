package com.mobilebytelabs.producttickets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilebytelabs.producttickets.domain.model.TicketCategory
import com.mobilebytelabs.producttickets.domain.model.TicketPriority
import com.mobilebytelabs.producttickets.domain.model.TicketType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreateTicketScreen(
    onBackClick: () -> Unit,
    ticketType: TicketType,
    viewModel: ProductTicketsViewModel = org.koin.compose.viewmodel.koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(ticketType) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            TicketCategory.entries.first { it.applicableTo.contains(ticketType) },
        )
    }
    var selectedPriority by remember { mutableStateOf(TicketPriority.MEDIUM) }
    var titleError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val availableCategories =
        TicketCategory.entries.filter { it.applicableTo.contains(selectedType) }

    LaunchedEffect(selectedType) {
        if (!availableCategories.contains(selectedCategory)) {
            selectedCategory = availableCategories.firstOrNull() ?: TicketCategory.GENERAL
        }
    }

    // Navigate back on successful submit
    LaunchedEffect(state.isSubmitting) {
        if (submitted && !state.isSubmitting && state.error == null) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ProductTicketsStrings.CREATE_TITLE) },
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
        ) {
            // Type
            Text(
                ProductTicketsStrings.CREATE_TYPE,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TicketType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text("${type.emoji} ${type.label}") },
                    )
                }
            }

            // Privacy notice for Contact Support
            if (selectedType == TicketType.CONTACT_SUPPORT) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = ProductTicketsStrings.SUPPORT_PRIVACY,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Category
            if (selectedType != TicketType.CONTACT_SUPPORT || availableCategories.size > 1) {
                Text(
                    ProductTicketsStrings.CREATE_CATEGORY,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    availableCategories.forEach { cat ->
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
                Spacer(Modifier.height(8.dp))
            }

            // Priority
            Text(
                ProductTicketsStrings.CREATE_PRIORITY,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TicketPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = {
                            Text(
                                "${priority.emoji} ${priority.label}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text(ProductTicketsStrings.CREATE_FIELD_TITLE) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(ProductTicketsStrings.CREATE_ERROR_TITLE) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    descriptionError = false
                },
                label = { Text(ProductTicketsStrings.CREATE_FIELD_DESCRIPTION) },
                isError = descriptionError,
                supportingText = if (descriptionError) {
                    { Text(ProductTicketsStrings.CREATE_ERROR_DESCRIPTION) }
                } else {
                    null
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = false
                },
                label = {
                    Text(
                        if (selectedType.isPrivate) {
                            ProductTicketsStrings.CREATE_FIELD_EMAIL_REQUIRED
                        } else {
                            ProductTicketsStrings.CREATE_FIELD_EMAIL_OPTIONAL
                        },
                    )
                },
                isError = emailError,
                supportingText = if (emailError) {
                    { Text(ProductTicketsStrings.CREATE_ERROR_EMAIL) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // Submit
            Button(
                onClick = {
                    titleError = title.isBlank()
                    descriptionError = description.isBlank()
                    emailError = selectedType.isPrivate && email.isBlank()
                    if (!titleError && !descriptionError && !emailError) {
                        submitted = true
                        viewModel.submitTicket(
                            selectedType,
                            title.trim(),
                            description.trim(),
                            selectedCategory.value,
                            selectedPriority.value,
                            email.ifBlank { null },
                        )
                    }
                },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        ProductTicketsStrings.CREATE_SUBMIT,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
