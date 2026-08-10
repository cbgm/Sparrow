package com.cbgm.securechat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.presentation.runtime.AppInitializationDependencies
import com.cbgm.securechat.presentation.runtime.ForegroundRuntimeDependencies
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppViewModel(
    private val initAppLanguageUseCase: InitAppLanguageUseCase,
    private val initialization: AppInitializationDependencies,
    private val foreground: ForegroundRuntimeDependencies
) : ViewModel() {
    private val logger = SecureChatLog.withTag("AppViewModel")
    private val isForeground = MutableStateFlow(false)
    private val isRuntimeReady = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            initializeApplication()
        }
        viewModelScope.launch {
            observeForegroundRuntime()
        }
    }

    fun onAppVisible() {
        foreground.appVisibilityState.onAppVisible()
        initialization.platformNotificationRuntime.requestPushTokenRegistration()
        isForeground.value = true
    }

    fun onAppHidden() {
        foreground.appVisibilityState.onAppHidden()
        isForeground.value = false
    }

    private suspend fun initializeApplication() {
        initialization.initializeCryptoRuntime()
            .getOrElse { error ->
                throw IllegalStateException(
                    "SecureChat could not initialize its cryptographic runtime",
                    error
                )
            }
        initAppLanguageUseCase()
        initialization.platformNotificationRuntime.initialize()
        initialization.conversationNotificationCoordinator.start()
        startControlPlaneMaintenance()
        observeControlPlaneRegistrationTargets()
        synchronizeDeviceContacts()
        isRuntimeReady.value = true
    }

    private fun startControlPlaneMaintenance() {
        viewModelScope.launch {
            while (isActive) {
                initialization.controlPlaneDirectorySynchronizer.refresh()
                delay(CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                initialization.controlPlaneHealthMonitor.refresh()
                delay(CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS)
            }
        }
    }

    private fun observeControlPlaneRegistrationTargets() {
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            combine(
                initialization.controlPlaneConfiguration.endpoints,
                initialization.controlPlaneStatusStore.statuses
            ) { endpoints, statuses ->
                val reachabilityByUrl =
                    statuses.associate { status ->
                        status.endpoint.baseUrl to status.reachability
                    }
                endpoints
                    .filter { endpoint ->
                        reachabilityByUrl[endpoint.baseUrl] != ControlPlaneReachability.UNREACHABLE
                    }.map { endpoint -> endpoint.baseUrl }
                    .toSet()
            }.distinctUntilChanged()
                .collectLatest {
                    initialization.platformNotificationRuntime.requestPushTokenRegistration()
                }
        }
    }

    private fun synchronizeDeviceContacts() {
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            if (!initialization.deviceContactsPermissionChecker.canReadContacts()) {
                logger.info { "Device contact sync skipped: READ_CONTACTS is not granted" }
                return@launch
            }

            initialization.importDeviceContacts()
                .onSuccess {
                    logger.info { "Device contact sync completed" }
                }.onFailure { error ->
                    logger.error(error) { "Device contact sync failed" }
                }
        }
    }

    private suspend fun waitUntilLocalIdentityIsReady() {
        initialization.observeLocalIdentityReady().first { ready -> ready }
    }

    private suspend fun observeForegroundRuntime() {
        combine(
            isForeground,
            isRuntimeReady
        ) { visible, ready ->
            visible && ready
        }.distinctUntilChanged()
            .collectLatest { shouldRun ->
                if (shouldRun) {
                    runForegroundSession()
                }
            }
    }

    private suspend fun runForegroundSession() {
        waitUntilLocalIdentityIsReady()
        foreground.incomingRelayRunner.start()
        foreground.relayConnectionManager.start()

        coroutineScope {
            val connectionObserver =
                launch {
                    foreground.relayConnectionManager.connectionState.collect(::handleConnectionState)
                }

            try {
                awaitCancellation()
            } finally {
                connectionObserver.cancelAndJoin()
                foreground.outboxRunner.stop()
                foreground.incomingRelayRunner.stop()
                foreground.relayConnectionManager.stop()
            }
        }
    }

    private suspend fun handleConnectionState(state: TransportConnectionState) {
        when (state) {
            is TransportConnectionState.Connected -> handleConnected(state)
            is TransportConnectionState.Connecting -> logger.debug { "Relay connecting" }
            is TransportConnectionState.Disconnected -> logger.info { "Relay disconnected" }
            is TransportConnectionState.Failed -> logger.error { "Relay failed: ${state.message}" }
        }
    }

    private suspend fun handleConnected(state: TransportConnectionState.Connected) {
        logger.info { "Relay connected: ${state.relayId}" }
        foreground.mailboxCoordinator
            .provisionRoutes()
            .onSuccess { provisioned ->
                logger.info { "Mailbox routes ready; newly provisioned=$provisioned" }
            }.onFailure { error ->
                logger.warn { "Mailbox route provisioning failed: ${error.message}" }
            }
        foreground.mailboxCoordinator
            .synchronizePending()
            .onSuccess { processed ->
                logger.info { "Mailbox synchronization completed; processed=$processed" }
            }.onFailure { error ->
                logger.warn { "Mailbox synchronization failed: ${error.message}" }
            }
        foreground.outboxRunner.start()
    }

    private companion object {
        const val CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS = 300_000L
        const val CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS = 60_000L
    }
}
