package com.cbgm.securechat.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cbgm.securechat.feature.chats.presentation.ChatRoute
import com.cbgm.securechat.feature.chats.presentation.GroupChatRoute
import com.cbgm.securechat.feature.chats.presentation.VerifyIdentityQrRoute
import com.cbgm.securechat.feature.chats.presentation.details.DetailsRoute
import com.cbgm.securechat.feature.chats.presentation.details.DetailsTarget
import com.cbgm.securechat.feature.contactimport.presentation.ImportIdentityRoute
import com.cbgm.securechat.feature.contactimport.presentation.ScanIdentityRoute
import com.cbgm.securechat.feature.contacts.presentation.BlockedContactsRoute
import com.cbgm.securechat.feature.contacts.presentation.ContactInvitationRoute
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute
import com.cbgm.securechat.feature.settings.presentation.ControlPlaneSettingsRoute
import com.cbgm.securechat.feature.settings.presentation.DeveloperMenuRoute
import com.cbgm.securechat.feature.settings.presentation.DisclaimerRoute
import com.cbgm.securechat.feature.settings.presentation.LicensesRoute
import com.cbgm.securechat.feature.settings.presentation.model.DisclaimerType
import com.cbgm.securechat.notification.application.ResolveNotificationConversation
import com.cbgm.securechat.notification.model.NotificationConversationTarget
import com.cbgm.securechat.notification.navigation.NotificationNavigationController
import com.cbgm.securechat.notification.navigation.NotificationNavigationTarget
import com.cbgm.securechat.presentation.MainRoute
import com.cbgm.securechat.startup.presentation.StartupRoute
import com.cbgm.securechat.startup.presentation.screen.component.SecureChatAppBackground
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    notificationNavigationController: NotificationNavigationController = koinInject(),
    resolveNotificationConversation: ResolveNotificationConversation = koinInject()
) {
    val navController = rememberNavController()
    val pendingNotificationTarget by notificationNavigationController.pendingTarget.collectAsStateWithLifecycle()
    var startupComplete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingNotificationTarget, startupComplete) {
        val target = pendingNotificationTarget ?: return@LaunchedEffect

        if (!startupComplete) {
            return@LaunchedEffect
        }

        when (target) {
            is NotificationNavigationTarget.Conversation -> {
                when (
                    val conversation =
                        resolveNotificationConversation(
                            conversationId = target.conversationId
                        )
                ) {
                    is NotificationConversationTarget.Direct -> {
                        navController.navigate(
                            AppDestination.Chat(
                                conversationId = conversation.conversationId,
                                contactId = conversation.contactId,
                                contactName = conversation.contactName
                            )
                        ) {
                            popUpTo(AppDestination.Main) {
                                inclusive = false
                            }

                            launchSingleTop = true
                        }
                    }

                    is NotificationConversationTarget.Group -> {
                        navController.navigate(
                            AppDestination.GroupConversation(
                                conversationId = conversation.conversationId
                            )
                        ) {
                            popUpTo(AppDestination.Main) {
                                inclusive = false
                            }

                            launchSingleTop = true
                        }
                    }

                    null -> Unit
                }
            }
        }

        notificationNavigationController.consume(target)
    }

    SecureChatAppBackground {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Startup,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.background
                    )
        ) {
            composable<AppDestination.Licences>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                }
            ) {
                LicensesRoute(onBack = { navController.popBackStack() })
            }

            composable<AppDestination.DeveloperMenu>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                }
            ) {
                DeveloperMenuRoute(onBack = { navController.popBackStack() })
            }

            composable<AppDestination.ControlPlanes>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
            ) {
                ControlPlaneSettingsRoute(onBack = { navController.popBackStack() })
            }

            composable<AppDestination.BlockedContacts>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                }
            ) {
                BlockedContactsRoute(onBack = { navController.popBackStack() })
            }

            composable<AppDestination.Disclaimer>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                }
            ) { backStackEntry ->
                val destination =
                    backStackEntry.toRoute<AppDestination.Disclaimer>()

                DisclaimerRoute(
                    type = destination.type,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<AppDestination.GroupConversation> { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.GroupConversation>()
                GroupChatRoute(
                    conversationId = destination.conversationId,
                    onClickHeader = {
                        navController.navigate(
                            AppDestination.Details(
                                child = DetailsChild.GROUP,
                                conversationId = destination.conversationId
                            )
                        )
                    },
                    onBack = {
                        navController.popBackStack(AppDestination.Main, false)
                    }
                )
            }

            composable<AppDestination.ShareIdentity> {
                ShareIdentityRoute(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.ImportContact> { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.ImportContact>()
                val scannedIdentityFromScanner by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("scannedIdentity", null)
                    .collectAsStateWithLifecycle()

                var destinationScannedIdentity by remember(destination.scannedIdentity) {
                    mutableStateOf(destination.scannedIdentity)
                }

                ImportIdentityRoute(
                    contactId = destination.contactId,
                    scannedIdentity = scannedIdentityFromScanner ?: destinationScannedIdentity,
                    onScannedIdentityConsumed = {
                        backStackEntry.savedStateHandle.remove<String>("scannedIdentity")
                        destinationScannedIdentity = null
                    },
                    onScanQrCode = {
                        navController.navigate(AppDestination.ScanIdentity)
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.Main> {
                MainRoute(
                    onOpenChat = { conversationId, contactId, contactName, isGroup ->
                        if (isGroup) {
                            navController.navigate(AppDestination.GroupConversation(conversationId))
                        } else {
                            navController.navigate(
                                AppDestination.Chat(
                                    conversationId = conversationId,
                                    contactId = contactId,
                                    contactName = contactName
                                )
                            )
                        }
                    },
                    onShareIdentity = {
                        navController.navigate(AppDestination.ShareIdentity)
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(
                            AppDestination.Disclaimer(
                                type = DisclaimerType.PRIVACY_POLICY
                            )
                        )
                    },
                    onNavigateToDataDisclaimer = {
                        navController.navigate(
                            AppDestination.Disclaimer(
                                type = DisclaimerType.DATA_DISCLAIMER
                            )
                        )
                    },
                    onNavigateToLicenses = {
                        navController.navigate(AppDestination.Licences)
                    },
                    onNavigateToDeveloperMenu = {
                        navController.navigate(AppDestination.DeveloperMenu)
                    },
                    onNavigateToControlPlanes = {
                        navController.navigate(AppDestination.ControlPlanes)
                    },
                    onImportContact = {
                        navController.navigate(AppDestination.ImportContact())
                    },
                    onNavigateToBlockedContacts = {
                        navController.navigate(AppDestination.BlockedContacts)
                    }
                )
            }

            composable<AppDestination.Chat>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                }
            ) { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.Chat>()

                ChatRoute(
                    conversationId = destination.conversationId,
                    contactId = destination.contactId,
                    contactName = destination.contactName,
                    onBack = {
                        navController.popBackStack(AppDestination.Main, false)
                    },
                    onClickHeader = {
                        navController.navigate(
                            AppDestination.Details(
                                child = DetailsChild.CONTACT,
                                conversationId = destination.conversationId,
                                contactId = destination.contactId
                            )
                        )
                    },
                    onVerifyIdentity = {
                        navController.navigate(
                            AppDestination.Details(
                                child = DetailsChild.CONTACT,
                                conversationId = destination.conversationId,
                                contactId = destination.contactId,
                                openVerification = true
                            )
                        )
                    },
                    onShareIdentity = {
                        navController.navigate(AppDestination.ShareIdentity)
                    },
                    onImportIdentity = {
                        navController.navigate(
                            AppDestination.ImportContact(contactId = destination.contactId)
                        )
                    }
                )
            }

            composable<AppDestination.Details>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                    )
                }
            ) { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.Details>()
                val verificationRevision by
                    backStackEntry.savedStateHandle
                        .getStateFlow("verificationRevision", 0)
                        .collectAsStateWithLifecycle()
                val detailsTarget =
                    when (destination.child) {
                        DetailsChild.CONTACT ->
                            DetailsTarget.Contact(
                                contactId = requireNotNull(destination.contactId)
                            )

                        DetailsChild.GROUP ->
                            DetailsTarget.Group(
                                conversationId = destination.conversationId
                            )
                    }

                DetailsRoute(
                    target = detailsTarget,
                    openVerification = destination.openVerification,
                    verificationRevision = verificationRevision,
                    onBack = {
                        navController.popBackStack()
                    },
                    onGroupLeft = {
                        navController.popBackStack(AppDestination.Main, false)
                    },
                    onScanContactQr = { contactId ->
                        navController.navigate(
                            AppDestination.VerifyIdentityQr(contactId = contactId)
                        )
                    },
                    onScanGroupMemberQr = { groupId, contactId ->
                        navController.navigate(
                            AppDestination.VerifyIdentityQr(
                                contactId = contactId,
                                groupId = groupId
                            )
                        )
                    }
                )
            }

            composable<AppDestination.VerifyIdentityQr> { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.VerifyIdentityQr>()

                VerifyIdentityQrRoute(
                    contactId = destination.contactId,
                    groupId = destination.groupId,
                    onVerified = {
                        if (destination.groupId == null) {
                            val previousEntry = navController.previousBackStackEntry
                            val previousRevision =
                                previousEntry
                                    ?.savedStateHandle
                                    ?.get<Int>("verificationRevision")
                                    ?: 0

                            previousEntry
                                ?.savedStateHandle
                                ?.set("verificationRevision", previousRevision + 1)
                        }

                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.ScanIdentity> {
                ScanIdentityRoute(
                    onQrCodeScanned = { encodedIdentity ->
                        /*
                         * Return to the import screen and provide the scanned
                         * payload through the saved-state handle.
                         */
                        navController
                            .previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(
                                "scannedIdentity",
                                encodedIdentity
                            )

                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.Startup> {
                StartupRoute(
                    onStartupComplete = {
                        startupComplete = true

                        navController.navigate(AppDestination.Main) {
                            popUpTo(AppDestination.Startup) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }

        ContactInvitationRoute()
    }
}
