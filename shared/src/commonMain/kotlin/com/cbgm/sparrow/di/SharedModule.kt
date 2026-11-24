package com.cbgm.sparrow.di

import com.cbgm.sparrow.core.util.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.util.phone.DigitPhoneNumberNormalizer
import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.transport.domain.port.HeadlessStartPort
import com.cbgm.sparrow.presentation.AppViewModel
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import com.cbgm.sparrow.runtime.AttachmentConversationNameObserver
import com.cbgm.sparrow.runtime.foreground.ForegroundRuntime
import com.cbgm.sparrow.runtime.startup.ApplicationStartupRunner
import com.cbgm.sparrow.runtime.startup.createApplicationStartupTasks
import com.cbgm.sparrow.startup.domain.runner.StartupRunner
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule =
    module {
        single<PhoneNumberNormalizer> { DigitPhoneNumberNormalizer() }
        single {
            AttachmentConversationNameObserver(conversationRepository = get(), contacts = get(), attachments = get())
        }
        single { ApplicationCoroutineScope() }
        single {
            AppInitializationDependencies(
                initializeCryptoRuntime = get(),
                resolveIdentityStatus = get(),
                initializeLocalEmbedding = get(),
                initializeSemanticSearch = get(),
                initializeMessageSafety = get(),
                platformNotificationRuntime = get(),
                conversationNotificationDispatcher = get(),
                attachmentConversationNameObserver = get(),
                invitationResultObserver = get(),
                membershipResultObserver = get(),
                messagingTransportResultObserver = get(),
                directIdentityResultObserver = get(),
                approvedIdentityReconnectionObserver = get(),
                contactBlockObserver = get(),
                pendingAuthorizationCleanupRunner = get(),
                recoverInterruptedDirectSends = get(),
                unpublishedGroupStateRunner = get(),
                workflowRegistry = get(),
                controlPlaneConfiguration = get(),
                controlPlaneStatus = get(),
                controlPlaneDirectorySynchronizer = get(),
                observeLocalIdentityReady = get(),
                importDeviceContacts = get(),
                deviceContactsPermissionChecker = get()
            )
        }

        single {
            ForegroundRuntimeDependencies(
                appVisibilityState = get(),
                transportConnection = get(),
                runCircuits = get(),
                outboxRunner = get(),
                mailboxPort = get(),
                observeIdentityKeysUsable = get(),
                observeWaitingForRoute = get(),
                sendUnsentMessages = get(),
                connectionKeepAlive = get()
            )
        }

        // A service started without the app (after a reboot, a check now and then) starts the runtime the same way.
        single<HeadlessStartPort> {
            val startupRunner = get<StartupRunner>()
            object : HeadlessStartPort {
                override suspend fun start() {
                    startupRunner.run()
                }
            }
        }

        single {
            ForegroundRuntime(
                foreground = get(),
                applicationScope = get<ApplicationCoroutineScope>()
            )
        }

        single<StartupRunner> {
            val initialization = get<AppInitializationDependencies>()

            ApplicationStartupRunner(
                startupTasks =
                    createApplicationStartupTasks(
                        initAppLanguageUseCase = get(),
                        initAppThemeModeUseCase = get(),
                        initialization = initialization
                    ),
                backgroundScope = get<ApplicationCoroutineScope>(),
                foregroundRuntime = get()
            )
        }

        viewModel {
            AppViewModel(
                foregroundRuntime = get(),
                recordAppHidden = get(),
                recordAppVisible = get(),
                observeAppLockEnabled = get()
            )
        }
    }
