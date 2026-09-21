package com.cbgm.sparrow.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.BuildKonfig
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import com.cbgm.sparrow.startup.util.StartupRuntimeReadiness
import kotlinx.coroutines.CancellationException
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
    private val foreground: ForegroundRuntimeDependencies,
    private val startupRuntimeReadiness: StartupRuntimeReadiness
) : ViewModel() {
    private val logger = SparrowLog.withTag("AppViewModel")
    private val isForeground = MutableStateFlow(false)
    private val isRuntimeReady = MutableStateFlow(false)

    var isLanguageInitialized by mutableStateOf(false)
        private set

    init {
        StartupTrace.begin()
        viewModelScope.launch {
            try {
                initializeApplication()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                startupRuntimeReadiness.markFailed(error)
                logger.error(error) { "Required application initialization failed" }
            }
        }
        viewModelScope.launch {
            observeForegroundRuntime()
        }
    }

    fun onAppVisible() {
        StartupTrace.event("app lifecycle visible")
        foreground.appVisibilityState.onAppVisible()
        initialization.platformNotificationRuntime.requestPushTokenRegistration()
        isForeground.value = true
    }

    fun onAppHidden() {
        foreground.appVisibilityState.onAppHidden()
        isForeground.value = false
    }

    private suspend fun initializeApplication() {
        StartupTrace.event("application initialization started")
        StartupTrace.measure("app language") { initAppLanguageUseCase() }
        isLanguageInitialized = true
        StartupTrace.event("language ready; navigation composition now permitted")

        StartupTrace.measure("crypto runtime") {
            initialization.initializeCryptoRuntime()
                .getOrElse { error ->
                    throw IllegalStateException(
                        "Sparrow could not initialize its cryptographic runtime",
                        error
                    )
                }
        }
        StartupTrace.measure("load saved control-plane configuration") {
            initialization.controlPlaneConfiguration.initialize()
        }
        StartupTrace.measure("notification runtime") {
            initialization.platformNotificationRuntime.initialize()
        }
        StartupTrace.measure("conversation notification coordinator") {
            initialization.conversationNotificationCoordinator.start()
        }
        StartupTrace.event("launching invitation/membership/identity result observers")
        startInvitationResultCoordinators()
        StartupTrace.measure("control-plane directory configuration/discovery") {
            initializeControlPlaneDirectory()
        }
        StartupTrace.event("starting background control-plane maintenance")
        startControlPlaneMaintenance()
        observeControlPlaneRegistrationTargets()
        synchronizeDeviceContacts()
        isRuntimeReady.value = true
        startupRuntimeReadiness.markReady()
        StartupTrace.event("app runtime ready; foreground session allowed")
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
                    logger.error(error) { "Control-plane directory configuration could not be stored" }
                }
        }

        // Persisted endpoints are sufficient to start transport. Do not block
        // the foreground runtime on an HTTP directory refresh or health probes
        // every time the app opens: startControlPlaneMaintenance() performs both.
        // A first run without any cached endpoints still needs initial discovery.
        val cachedEndpointCount = initialization.controlPlaneConfiguration.endpoints.value.size
        StartupTrace.event("saved control-plane endpoints=$cachedEndpointCount")
        if (cachedEndpointCount == 0) {
            initialization.controlPlaneDirectorySynchronizer
                .refresh()
                .onSuccess { count ->
                    logger.info { "Initial control-plane directory synchronized; addresses=$count" }
                }.onFailure { error ->
                    logger.error(error) { "Initial control-plane directory unavailable" }
                }
        }
    }

    private fun startInvitationResultCoordinators() {
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            initialization.attachmentConversationNameObserver.run()
        }
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            initialization.invitationResultObserver.run()
        }
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            initialization.membershipResultObserver.run()
        }
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            initialization.messagingTransportResultObserver.run()
        }
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            initialization.directIdentityResultObserver.run()
        }
        viewModelScope.launch {
            waitUntilLocalIdentityIsReady()
            initialization.contactBlockObserver.run()
        }
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
        StartupTrace.event("foreground session requested; waiting for local identity")
        StartupTrace.measure("foreground local identity ready") { waitUntilLocalIdentityIsReady() }
        StartupTrace.measure("incoming envelope runner start") { foreground.incomingEnvelopeRunner.start() }
        StartupTrace.measure("transport connection manager start") { foreground.transportConnectionManager.start() }

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
        StartupTrace.event(
            "foreground transport state=${when (state) {
                is TransportConnectionState.Connected -> "Connected"
                is TransportConnectionState.Connecting -> "Connecting"
                is TransportConnectionState.Disconnected -> "Disconnected"
                is TransportConnectionState.Failed -> "Failed"
            }}"
        )
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
                logger.error(error) { "Mailbox route provisioning failed" }
            }
        foreground.mailboxCoordinator
            .synchronizePending()
            .onSuccess { processed ->
                logger.info { "Mailbox synchronization completed; processed=$processed" }
            }.onFailure { error ->
                logger.error(error) { "Mailbox synchronization failed" }
            }
        foreground.outboxRunner.start()
    }

    private companion object {
        const val CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS = 300_000L
        const val CONTROL_PLANE_DIRECTORY_RETRY_MILLISECONDS = 5_000L
        const val CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS = 60_000L
    }
}
