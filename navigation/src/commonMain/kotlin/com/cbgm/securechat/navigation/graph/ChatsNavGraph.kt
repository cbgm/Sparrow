package com.cbgm.securechat.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.feature.chats.presentation.ChatRoute
import com.cbgm.securechat.feature.chats.presentation.GroupChatRoute
import com.cbgm.securechat.feature.chats.presentation.VerifyIdentityQrRoute
import com.cbgm.securechat.feature.chats.presentation.details.DetailsRoute
import com.cbgm.securechat.feature.chats.presentation.details.DetailsTarget
import com.cbgm.securechat.navigation.slideInFromLeft
import com.cbgm.securechat.navigation.slideInFromRight
import com.cbgm.securechat.navigation.slideOutToLeft
import com.cbgm.securechat.navigation.slideOutToRight

fun NavGraphBuilder.chatsNavGraph() {
    composable<AppRoute.Chat>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.Chat>()

        ChatRoute(
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
        GroupChatRoute(conversationId = destination.conversationId)
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
            openVerification = false
        )
    }

    composable<AppRoute.VerifyIdentityQr> { backStackEntry ->
        val destination = backStackEntry.toRoute<AppRoute.VerifyIdentityQr>()
        VerifyIdentityQrRoute(
            contactId = destination.contactId,
            groupId = destination.groupId
        )
    }
}
