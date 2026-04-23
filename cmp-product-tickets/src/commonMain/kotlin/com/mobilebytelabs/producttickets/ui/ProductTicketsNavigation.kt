package com.mobilebytelabs.producttickets.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mobilebytelabs.producttickets.domain.model.TicketType
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object ProductTicketsRoute

@Serializable
data class CreateTicketRoute(val ticketType: String = TicketType.FEATURE_REQUEST.value)

@Serializable
data class TicketDetailRoute(val ticketId: String)

fun NavController.navigateToProductTickets(navOptions: NavOptions? = null) {
    navigate(ProductTicketsRoute, navOptions)
}

fun NavController.navigateToCreateTicket(type: TicketType) {
    navigate(CreateTicketRoute(ticketType = type.value))
}

fun NavController.navigateToTicketDetail(ticketId: String) {
    navigate(TicketDetailRoute(ticketId = ticketId))
}

fun NavGraphBuilder.productTicketsDestination(
    onBackClick: () -> Unit,
    onNavigateToCreateTicket: (TicketType) -> Unit,
    onNavigateToTicketDetail: (String) -> Unit,
) {
    composable<ProductTicketsRoute> {
        ProductTicketsScreen(
            onBackClick = onBackClick,
            onNavigateToCreateTicket = onNavigateToCreateTicket,
            onNavigateToTicketDetail = onNavigateToTicketDetail,
        )
    }
}

fun NavGraphBuilder.createTicketDestination(onBackClick: () -> Unit) {
    composable<CreateTicketRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CreateTicketRoute>()
        val ticketType = TicketType.entries.find { it.value == route.ticketType }
            ?: TicketType.FEATURE_REQUEST
        CreateTicketScreen(
            onBackClick = onBackClick,
            ticketType = ticketType,
        )
    }
}

fun NavGraphBuilder.ticketDetailDestination(onBackClick: () -> Unit) {
    composable<TicketDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<TicketDetailRoute>()
        val viewModel: ProductTicketsViewModel = koinViewModel()
        val ticket = viewModel.getTicketById(route.ticketId)
        if (ticket != null) {
            TicketDetailScreen(
                onBackClick = onBackClick,
                ticket = ticket,
                onUpvote = { viewModel.upvoteTicket(ticket.id) },
            )
        }
    }
}
