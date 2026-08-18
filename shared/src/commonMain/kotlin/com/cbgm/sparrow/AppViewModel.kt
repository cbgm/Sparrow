package com.cbgm.sparrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.presentation.runtime.AppInitializationDependencies
import com.cbgm.sparrow.presentation.runtime.ForegroundRuntimeDependencies
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
import kotlin.time.Duration.Companion.milliseconds

class AppViewModel(
    private val initAppLanguageUseCase: InitAppLanguageUseCase,
    private val initialization: AppInitializationDependencies,
    private val foreground: ForegroundRuntimeDependencies
) : ViewModel() {
    private val logger = SparrowLog.withTag("AppViewModel")
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
                    "Sparrow could not initialize its cryptographic runtime",
                    error
                )
            }
        initAppLanguageUseCase()
        initialization.controlPlaneConfiguration.initialize()
        initialization.platformNotificationRuntime.initialize()
        initialization.conversationNotificationCoordinator.start()
        initializeControlPlaneDirectory()
        startControlPlaneMaintenance()
        observeControlPlaneRegistrationTargets()
        synchronizeDeviceContacts()
        isRuntimeReady.value = true
    }

    private suspend fun initializeControlPlaneDirectory() {
        val configuredDirectoryUrl =
            BuildKonfig.CONTROL_PLANE_DIRECTORY_URL
                .trim()
                .takeIf(String::isNotBlank)
        if (initialization.controlPlaneConfiguration.directoryUrl.value == null &&
            configuredDirectoryUrl != null
        ) {
            initialization.controlPlaneConfiguration
                .setDirectoryUrl(configuredDirectoryUrl)
                .onFailure { error ->
                    logger.warn {
                        "Control-plane directory configuration could not be stored: ${error.message}"
                    }
                }
        }

        initialization.controlPlaneDirectorySynchronizer
            .refresh()
            .onSuccess { count ->
                logger.info { "Control-plane directory synchronized; addresses=$count" }
            }.onFailure { error ->
                logger.warn {
                    "Control-plane directory unavailable during startup: ${error.message}"
                }
            }
        initialization.controlPlaneHealthMonitor.refresh()
    }

    private fun startControlPlaneMaintenance() {
        viewModelScope.launch {
            while (isActive) {
                val result = initialization.controlPlaneDirectorySynchronizer.refresh()
                delay(
                    (
                        if (result.isSuccess) {
                            CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS
                        } else {
                            CONTROL_PLANE_DIRECTORY_RETRY_MILLISECONDS
                        }
                    ).milliseconds
                )
            }
        }
        viewModelScope.launch {
            while (isActive) {
                initialization.controlPlaneHealthMonitor.refresh()
                delay(CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS.milliseconds)
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
        foreground.incomingEnvelopeRunner.start()
        foreground.transportConnectionManager.start()

        coroutineScope {
            val connectionObserver =
                launch {
                    foreground.transportConnectionManager.connectionState.collect(::handleConnectionState)
                }

            try {
                awaitCancellation()
            } finally {
                connectionObserver.cancelAndJoin()
                foreground.outboxRunner.stop()
                foreground.incomingEnvelopeRunner.stop()
                foreground.transportConnectionManager.stop()
            }
        }
    }

    private suspend fun handleConnectionState(state: TransportConnectionState) {
        when (state) {
            is TransportConnectionState.Connected -> handleConnected(state)
            is TransportConnectionState.Connecting -> logger.debug { "Transport connecting" }
            is TransportConnectionState.Disconnected -> logger.info { "Transport disconnected" }
            is TransportConnectionState.Failed -> logger.error { "Transport failed: ${state.message}" }
        }
    }

    private suspend fun handleConnected(state: TransportConnectionState.Connected) {
        logger.info { "Transport connected: ${state.routingId}" }
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
        const val CONTROL_PLANE_DIRECTORY_RETRY_MILLISECONDS = 5_000L
        const val CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS = 60_000L
    }
}
