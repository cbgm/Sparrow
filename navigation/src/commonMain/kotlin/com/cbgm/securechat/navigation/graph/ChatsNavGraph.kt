package com.cbgm.securechat.navigation.graph

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val verificationRevision by
            backStackEntry.savedStateHandle
                .getStateFlow(VERIFICATION_REVISION_KEY, 0)
                .collectAsStateWithLifecycle()

        DetailsRoute(
            target = DetailsTarget.Contact(contactId = destination.contactId),
            openVerification = destination.openVerification,
            verificationRevision = verificationRevision
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
            verificationRevision = 0
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

private const val VERIFICATION_REVISION_KEY = "verificationRevision"
