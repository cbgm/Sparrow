package com.cbgm.sparrow.navigation.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import com.cbgm.sparrow.navigation.routing.graph.attachmentsNavGraph
import com.cbgm.sparrow.navigation.routing.graph.chatsNavGraph
import com.cbgm.sparrow.navigation.routing.graph.contactsNavGraph
import com.cbgm.sparrow.navigation.routing.graph.identityNavGraph
import com.cbgm.sparrow.navigation.routing.graph.inviteNavGraph
import com.cbgm.sparrow.navigation.routing.graph.mainNavGraph
import com.cbgm.sparrow.navigation.routing.graph.mediaNavGraph
import com.cbgm.sparrow.navigation.routing.graph.settingsNavGraph
import com.cbgm.sparrow.navigation.routing.graph.startupNavGraph
import com.cbgm.sparrow.notification.domain.model.NotificationConversationTarget
import com.cbgm.sparrow.notification.domain.usecase.ResolveNotificationConversationUseCase
import com.cbgm.sparrow.notification.presentation.navigation.NotificationNavigationController
import com.cbgm.sparrow.notification.presentation.navigation.NotificationNavigationTarget
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.app_connection_offline_hint
import com.cbgm.sparrow.resources.app_connection_reconnected_hint
import com.cbgm.sparrow.startup.domain.model.AppConnectionAvailability
import com.cbgm.sparrow.startup.domain.usecase.ObserveAppConnectionAvailabilityUseCase
import com.cbgm.sparrow.startup.presentation.start.model.StartupConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

private const val OFFLINE_HINT_DELAY_MILLIS = 5_000L

@Composable
fun AppNavigation(
    onStartupContentReady: () -> Unit = {},
    notificationNavigationController: NotificationNavigationController = koinInject(),
    resolveNotificationConversation: ResolveNotificationConversationUseCase = koinInject(),
    navigator: AppNavigator = koinInject(),
    observeAppConnectionAvailability: ObserveAppConnectionAvailabilityUseCase = koinInject()
) {
    val navController = rememberNavController()

    val pendingNotificationTarget by notificationNavigationController
        .pendingTarget
        .collectAsStateWithLifecycle()

    var startupComplete by rememberSaveable {
        mutableStateOf(false)
    }

    var startupWasOffline by rememberSaveable {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    navController.bind(navigator)

    ObserveConnectionSnackbar(
        startupComplete = startupComplete,
        startupWasOffline = startupWasOffline,
        snackbarHostState = snackbarHostState,
        observeAppConnectionAvailability = observeAppConnectionAvailability
    )

    HandlePendingNotificationNavigation(
        pendingNotificationTarget = pendingNotificationTarget,
        startupComplete = startupComplete,
        notificationNavigationController = notificationNavigationController,
        resolveNotificationConversation = resolveNotificationConversation,
        navigator = navigator
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Startup,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            startupNavGraph(
                onStartupReady = { connection ->
                    startupWasOffline =
                        connection == StartupConnection.OFFLINE

                    startupComplete = true
                },
                onStartupContentReady = onStartupContentReady
            )

            mainNavGraph(
                onMainReady = onStartupContentReady
            )

            chatsNavGraph()
            attachmentsNavGraph()
            mediaNavGraph()
            contactsNavGraph()
            inviteNavGraph()
            identityNavGraph()
            settingsNavGraph()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ObserveConnectionSnackbar(
    startupComplete: Boolean,
    startupWasOffline: Boolean,
    snackbarHostState: SnackbarHostState,
    observeAppConnectionAvailability: ObserveAppConnectionAvailabilityUseCase
) {
    val offlineHint = stringResource(
        Res.string.app_connection_offline_hint
    )

    val reconnectedHint = stringResource(
        Res.string.app_connection_reconnected_hint
    )

    LaunchedEffect(
        startupComplete,
        offlineHint,
        reconnectedHint
    ) {
        if (!startupComplete) return@LaunchedEffect

        var connectionUnavailable = startupWasOffline
        var offlineHintWasShown = false

        var pendingOfflineHintJob: Job? = null
        var connectionSnackbarJob: Job? = null

        fun scheduleOfflineHint() {
            pendingOfflineHintJob?.cancel()

            pendingOfflineHintJob = launch {
                // Do not report a transient Connecting state as offline.
                delay(OFFLINE_HINT_DELAY_MILLIS.milliseconds)

                offlineHintWasShown = true

                connectionSnackbarJob?.cancel()

                connectionSnackbarJob = launch {
                    snackbarHostState.showSnackbar(
                        message = offlineHint,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

        if (connectionUnavailable) {
            scheduleOfflineHint()
        }

        observeAppConnectionAvailability().collect { availability ->
            when {
                availability == AppConnectionAvailability.UNAVAILABLE &&
                    !connectionUnavailable -> {
                    connectionUnavailable = true
                    offlineHintWasShown = false

                    connectionSnackbarJob?.cancel()

                    scheduleOfflineHint()
                }

                availability == AppConnectionAvailability.AVAILABLE &&
                    connectionUnavailable -> {
                    connectionUnavailable = false

                    pendingOfflineHintJob?.cancel()
                    connectionSnackbarJob?.cancel()

                    // Do not announce a reconnection if the offline hint
                    // was never displayed.
                    if (offlineHintWasShown) {
                        offlineHintWasShown = false

                        connectionSnackbarJob = launch {
                            snackbarHostState.showSnackbar(
                                message = reconnectedHint,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandlePendingNotificationNavigation(
    pendingNotificationTarget: NotificationNavigationTarget?,
    startupComplete: Boolean,
    notificationNavigationController: NotificationNavigationController,
    resolveNotificationConversation: ResolveNotificationConversationUseCase,
    navigator: AppNavigator
) {
    LaunchedEffect(
        pendingNotificationTarget,
        startupComplete
    ) {
        val target = pendingNotificationTarget
            ?: return@LaunchedEffect

        if (!startupComplete) return@LaunchedEffect

        when (target) {
            is NotificationNavigationTarget.Conversation -> {
                when (
                    val conversation = resolveNotificationConversation(
                        conversationId = target.conversationId
                    )
                ) {
                    is NotificationConversationTarget.Direct -> {
                        navigator.navigateTo(
                            route = AppRoute.Chat(
                                conversationId = conversation.conversationId,
                                contactId = conversation.contactId,
                                contactName = conversation.contactName
                            ),
                            popUpTo = AppRoute.Main
                        )
                    }

                    is NotificationConversationTarget.Group -> {
                        navigator.navigateTo(
                            route = AppRoute.GroupConversation(
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
}
