package com.cbgm.sparrow.runtime.foreground

import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.feature.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private suspend fun observeForegroundState() {
        isForeground.collectLatest { shouldRun ->
            if (shouldRun) {
                runForegroundSession()
            }
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
}
