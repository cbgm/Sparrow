package com.cbgm.sparrow.di

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.presentation.AppViewModel
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule =
    module {
        single { ApplicationCoroutineScope() }

        single {
            AppInitializationDependencies(
                initializeCryptoRuntime = get(),
                platformNotificationRuntime = get(),
                conversationNotificationCoordinator = get(),
                directInvitationConversationCoordinator = get(),
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

        viewModel {
            AppViewModel(
                initAppLanguageUseCase = get(),
                initialization = get(),
                foreground = get()
            )
        }
    }
