package com.cbgm.sparrow.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cbgm.sparrow.core.ui.navigation.AppNavigator
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.navigation.graph.chatsNavGraph
import com.cbgm.sparrow.navigation.graph.contactsNavGraph
import com.cbgm.sparrow.navigation.graph.identityNavGraph
import com.cbgm.sparrow.navigation.graph.mainNavGraph
import com.cbgm.sparrow.navigation.graph.settingsNavGraph
import com.cbgm.sparrow.navigation.graph.startupNavGraph
import com.cbgm.sparrow.notification.application.ResolveNotificationConversation
import com.cbgm.sparrow.notification.model.NotificationConversationTarget
import com.cbgm.sparrow.notification.navigation.NotificationNavigationController
import com.cbgm.sparrow.notification.navigation.NotificationNavigationTarget
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_startup_offline_hint
import com.cbgm.sparrow.startup.presentation.model.StartupConnection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    notificationNavigationController: NotificationNavigationController = koinInject(),
    resolveNotificationConversation: ResolveNotificationConversation = koinInject(),
    navigator: AppNavigator = koinInject()
) {
    val navController = rememberNavController()
    val pendingNotificationTarget by notificationNavigationController.pendingTarget.collectAsStateWithLifecycle()
    var startupComplete by rememberSaveable { mutableStateOf(false) }
    var showOfflineHint by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineHint = stringResource(Res.string.feature_startup_offline_hint)

    navController.bind(navigator)

    LaunchedEffect(showOfflineHint) {
        if (!showOfflineHint) return@LaunchedEffect
        showOfflineHint = false
        snackbarHostState.showSnackbar(offlineHint)
    }

    LaunchedEffect(pendingNotificationTarget, startupComplete) {
        val target = pendingNotificationTarget ?: return@LaunchedEffect
        if (!startupComplete) return@LaunchedEffect

        when (target) {
            is NotificationNavigationTarget.Conversation -> {
                when (
                    val conversation =
                        resolveNotificationConversation(
                            conversationId = target.conversationId
                        )
                ) {
                    is NotificationConversationTarget.Direct -> {
                        navigator.navigateTo(
                            route =
                                AppRoute.Chat(
                                    conversationId = conversation.conversationId,
                                    contactId = conversation.contactId,
                                    contactName = conversation.contactName
                                ),
                            popUpTo = AppRoute.Main
                        )
                    }

                    is NotificationConversationTarget.Group -> {
                        navigator.navigateTo(
                            route =
                                AppRoute.GroupConversation(
                                    conversationId = conversation.conversationId
                                ),
                            popUpTo = AppRoute.Main
                        )
                    }

                    null -> Unit
                }
            }
        }

        notificationNavigationController.consume(target)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Startup,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
        ) {
            startupNavGraph(
                onStartupReady = { connection ->
                    startupComplete = true
                    showOfflineHint = connection == StartupConnection.OFFLINE
                }
            )
            mainNavGraph()
            chatsNavGraph()
            contactsNavGraph()
            identityNavGraph()
            settingsNavGraph()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
