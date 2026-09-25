package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.chats.presentation.details.model.DetailsTarget
import com.cbgm.sparrow.feature.chats.presentation.details.screen.DetailsRoute
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectConversationRoute
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationRoute
import com.cbgm.sparrow.feature.chats.presentation.verification.VerificationRoute
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ReconnectExistingConversationUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.MessageSafetyDetailsRoute
import com.cbgm.sparrow.feature.search.presentation.overview.MessageSearchRoute
import com.cbgm.sparrow.navigation.routing.slideInFromLeft
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToLeft
import com.cbgm.sparrow.navigation.routing.slideOutToRight
import org.koin.compose.koinInject

fun NavGraphBuilder.chatsNavGraph() {
    composable<AppRoute.MessageSearch>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        MessageSearchRoute()
    }

    composable<AppRoute.MessageSafetyDetails>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        MessageSafetyDetailsRoute()
    }

    composable<AppRoute.Chat>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.Chat>()

        // The ViewModel needs both the navigation arguments and the restored draft state.
        // Pass this handle as a Koin parameter instead of resolving it from the root scope.
        val savedStateHandle = backStackEntry.savedStateHandle.apply {
            set(AppRoute.Chat::conversationId.name, destination.conversationId)
            set(AppRoute.Chat::contactId.name, destination.contactId)
            set(AppRoute.Chat::contactName.name, destination.contactName)
            set(AppRoute.Chat::targetMessageId.name, destination.targetMessageId)
        }

        val reconnectExistingConversation: ReconnectExistingConversationUseCase = koinInject()
        DirectConversationRoute(
            onRequestReconnect = { peerId -> reconnectExistingConversation(peerId) },
            contactId = destination.contactId,
            targetMessageId = destination.targetMessageId,
            savedStateHandle = savedStateHandle
        )
    }

    composable<AppRoute.GroupConversation>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.GroupConversation>()
        GroupConversationRoute(
            conversationId = destination.conversationId,
            targetMessageId = destination.targetMessageId
        )
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
            groupId = destination.groupId
        )
    }
}
