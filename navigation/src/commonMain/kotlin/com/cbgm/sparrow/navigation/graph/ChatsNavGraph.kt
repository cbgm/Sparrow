package com.cbgm.sparrow.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.chats.presentation.details.model.DetailsTarget
import com.cbgm.sparrow.feature.chats.presentation.details.screen.DetailsRoute
import com.cbgm.sparrow.feature.chats.presentation.direct.screen.DirectRoute
import com.cbgm.sparrow.feature.chats.presentation.group.screen.GroupRoute
import com.cbgm.sparrow.feature.chats.presentation.verification.VerificationRoute
import com.cbgm.sparrow.navigation.slideInFromLeft
import com.cbgm.sparrow.navigation.slideInFromRight
import com.cbgm.sparrow.navigation.slideOutToLeft
import com.cbgm.sparrow.navigation.slideOutToRight

fun NavGraphBuilder.chatsNavGraph() {
    composable<AppRoute.Chat>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.Chat>()

        DirectRoute(
            conversationId = destination.conversationId,
            contactId = destination.contactId,
            contactName = destination.contactName
        )
    }

    composable<AppRoute.GroupConversation>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.GroupConversation>()
        GroupRoute(conversationId = destination.conversationId)
    }

    composable<AppRoute.ContactDetails>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToLeft() },
        popEnterTransition = { slideInFromLeft() },
        popExitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.ContactDetails>()

        DetailsRoute(
            target = DetailsTarget.Contact(contactId = destination.contactId),
            openVerification = destination.openVerification
        )
    }

    composable<AppRoute.GroupDetails>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToLeft() },
        popEnterTransition = { slideInFromLeft() },
        popExitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.GroupDetails>()

        DetailsRoute(
            target = DetailsTarget.Group(conversationId = destination.conversationId),
            openVerification = false,
            requestGroupLeave = destination.requestLeave
        )
    }

    composable<AppRoute.VerifyIdentityQr> { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.VerifyIdentityQr>()
        VerificationRoute(
            contactId = destination.contactId,
            groupId = destination.groupId
        )
    }
}
