package com.cbgm.securechat.di

import com.cbgm.securechat.AppViewModel
import com.cbgm.securechat.presentation.runtime.AppInitializationDependencies
import com.cbgm.securechat.presentation.runtime.ForegroundRuntimeDependencies
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule =
    module {
        single {
            AppInitializationDependencies(
                initializeCryptoRuntime = get(),
                platformNotificationRuntime = get(),
                conversationNotificationCoordinator = get(),
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
                incomingRelayRunner = get(),
                relayConnectionManager = get(),
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
