package com.cbgm.sparrow.di

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.phone.DefaultPhoneNumberNormalizer
import com.cbgm.sparrow.core.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.presentation.AppViewModel
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import com.cbgm.sparrow.runtime.AttachmentConversationNameObserver
import com.cbgm.sparrow.runtime.foreground.ForegroundRuntimeCoordinator
import com.cbgm.sparrow.runtime.startup.ApplicationStartupRunner
import com.cbgm.sparrow.runtime.startup.createApplicationStartupTasks
import com.cbgm.sparrow.startup.domain.runner.StartupRunner
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule =
    module {
        single<PhoneNumberNormalizer> { DefaultPhoneNumberNormalizer() }
        single { AttachmentConversationNameObserver(conversationRepository = get(), contacts = get(), attachments = get()) }
        single { ApplicationCoroutineScope() }
        single {
            AppInitializationDependencies(
                initializeCryptoRuntime = get(),
                getIdentityStatus = get(),
                recoverIncompleteIdentity = get(),
                initializeLocalEmbedding = get(),
                initializeSemanticSearch = get(),
                initializeMessageSafety = get(),
                platformNotificationRuntime = get(),
                conversationNotificationCoordinator = get(),
                attachmentConversationNameObserver = get(),
                invitationResultObserver = get(),
                membershipResultObserver = get(),
                messagingTransportResultObserver = get(),
                directIdentityResultObserver = get(),
                approvedIdentityReconnectionObserver = get(),
                contactBlockObserver = get(),
                controlPlaneConfiguration = get(),
                controlPlaneStatusStore = get(),
                controlPlaneDirectorySynchronizer = get(),
                controlPlaneHealthMonitor = get(),
                observeLocalIdentityReady = get(),
                importDeviceContacts = get(),
                deviceContactsPermissionChecker = get()
            )
        }

        single {
            ForegroundRuntimeDependencies(
                appVisibilityState = get(),
                incomingEnvelopeRunner = get(),
                transportConnectionManager = get(),
                outboxRunner = get(),
                mailboxCoordinator = get()
            )
        }

        single {
            ForegroundRuntimeCoordinator(
                foreground = get(),
                notificationRuntime = get(),
                applicationScope = get<ApplicationCoroutineScope>()
            )
        }

        single<StartupRunner> {
            val initialization = get<AppInitializationDependencies>()

            ApplicationStartupRunner(
                startupTasks =
                    createApplicationStartupTasks(
                        initAppLanguageUseCase = get(),
                        initialization = initialization
                    ),
                backgroundScope = get<ApplicationCoroutineScope>(),
                foregroundRuntime = get()
            )
        }

        viewModel {
            AppViewModel(
                foregroundRuntime = get()
            )
        }
    }
