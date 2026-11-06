package com.cbgm.sparrow.runtime.foreground

import com.cbgm.sparrow.core.util.logging.StartupTrace
import com.cbgm.sparrow.feature.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ForegroundRuntimeCoordinator(
    private val foreground: ForegroundRuntimeDependencies,
    private val notificationRuntime: PlatformNotificationRuntime,
    private val applicationScope: CoroutineScope
) {
    private val isForeground = MutableStateFlow(false)
    private val connectionHandler = ForegroundConnectionHandler(foreground)
    private var runtimeJob: Job? = null

    fun start() {
        if (runtimeJob != null) return

        runtimeJob =
            applicationScope.launch {
                observeForegroundState()
            }
    }

    fun onAppVisible() {
        if (isForeground.value) return

        StartupTrace.event("app lifecycle visible")
        foreground.appVisibilityState.onAppVisible()
        notificationRuntime.requestPushTokenRegistration()
        isForeground.value = true
    }

    fun onAppHidden() {
        if (!isForeground.value) return

        foreground.appVisibilityState.onAppHidden()
        isForeground.value = false
    }

    // Without usable identity keys the connection and runners pause until the keys are restored.
    private suspend fun observeForegroundState() {
        combine(isForeground, foreground.observeIdentityKeysUsable()) { visible, keysUsable -> visible && keysUsable }
            .distinctUntilChanged()
            .collectLatest { shouldRun ->
                if (shouldRun) {
                    runForegroundSession()
                }
            }
    }

    /**
     * Messages held back while there was no circuit through enough hops (sending directly switched off) are retried
     * by the outbox with a growing delay; once such a circuit is up they go out at once.
     */
    private suspend fun sendWaitingMessagesWhenRouteReturns() {
        var waiting = false
        foreground.observeWaitingForRoute().distinctUntilChanged().collect { waitingNow ->
            if (waiting && !waitingNow) foreground.outboxRunner.start()
            waiting = waitingNow
        }
    }

    private suspend fun runForegroundSession() {
        StartupTrace.measure("incoming envelope runner start") {
            foreground.incomingEnvelopeRunner.start()
        }
        StartupTrace.measure("transport connection manager start") {
            foreground.transportConnectionManager.start()
        }

        coroutineScope {
            val connectionObserver =
                launch {
                    foreground.transportConnectionManager.connectionState
                        .collect(connectionHandler::handle)
                }
            // Mailbox requests and messages to contacts with a mailbox go through the circuit.
            val circuit = launch { foreground.circuitRunner.run() }
            val mailboxArrivals = launch { foreground.mailboxCoordinator.listen() }
            val routeReturns = launch { sendWaitingMessagesWhenRouteReturns() }

            try {
                awaitCancellation()
            } finally {
                connectionObserver.cancelAndJoin()
                routeReturns.cancelAndJoin()
                mailboxArrivals.cancelAndJoin()
                circuit.cancelAndJoin()
                foreground.outboxRunner.stop()
                foreground.incomingEnvelopeRunner.stop()
                foreground.transportConnectionManager.stop()
            }
        }
    }
}
