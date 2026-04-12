package com.mobilebytesensei.featurerequest.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object FeatureWishlistRoute

fun NavController.navigateToFeatureWishlist(navOptions: NavOptions? = null) {
    navigate(FeatureWishlistRoute, navOptions)
}

fun NavGraphBuilder.featureWishlistDestination(
    onBackClick: () -> Unit,
) {
    composable<FeatureWishlistRoute> {
        UserTicketsScreen(
            onBackClick = onBackClick,
        )
    }
}
