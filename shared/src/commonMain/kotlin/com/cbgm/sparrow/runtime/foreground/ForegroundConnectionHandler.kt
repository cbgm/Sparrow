package com.cbgm.sparrow.runtime.foreground

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.feature.transport.connection.isRecoverableConnectivityFailure
import com.cbgm.sparrow.presentation.model.ForegroundRuntimeDependencies
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class ForegroundConnectionHandler(
    private val foreground: ForegroundRuntimeDependencies
) {
    private val logger = SparrowLog.withTag("ForegroundConnectionHandler")

    suspend fun handle(state: TransportConnectionState) {
        StartupTrace.event("foreground transport state=${state.traceName()}")

        when (state) {
            is TransportConnectionState.Connected -> handleConnected(state)
            is TransportConnectionState.Connecting ->
                logger.debug { "Transport connecting" }

            is TransportConnectionState.Disconnected ->
                logger.info { "Transport disconnected" }

            is TransportConnectionState.Failed ->
                logger.debug { "Transport unavailable: ${state.message}" }
        }
    }

    private suspend fun handleConnected(
        state: TransportConnectionState.Connected
    ) {
        logger.info { "Transport connected: ${state.routingId}" }

        val initialProvisioning = foreground.mailboxCoordinator.provisionRoutes()
        val initialFailure = initialProvisioning.exceptionOrNull()
        if (initialFailure is CancellationException) throw initialFailure

        initialProvisioning
            .onSuccess { provisioned ->
                logger.info {
                    "Mailbox routes ready; newly provisioned=$provisioned"
                }
            }.onFailure { error ->
                SparrowLog.diagnostic(
                    "ForegroundConnectionHandler",
                    "Mailbox route provisioning deferred",
                    error
                )
            }

        foreground.mailboxCoordinator.synchronizePending()
            .onSuccess { processed ->
                logger.info {
                    "Mailbox synchronization completed; processed=$processed"
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                SparrowLog.diagnostic(
                    "ForegroundConnectionHandler",
                    "Mailbox synchronization deferred",
                    error
                )
            }

        foreground.outboxRunner.start()

        if (
            initialFailure != null &&
            initialFailure.isTransientMailboxNetworkFailure()
        ) {
            retryMailboxProvisioning()
        }
    }

    private suspend fun retryMailboxProvisioning() {
        var backoffMilliseconds =
            MAILBOX_PROVISIONING_INITIAL_RETRY_MILLISECONDS
        var attempt = 0

        while (true) {
            delay(backoffMilliseconds.milliseconds)

            val result = foreground.mailboxCoordinator.provisionRoutes()
            val failure = result.exceptionOrNull()
            if (failure is CancellationException) throw failure

            if (failure == null) {
                logger.info {
                    "Mailbox route provisioning recovered; " +
                        "newly provisioned=${result.getOrThrow()}"
                }
                return
            }

            attempt += 1
            if (
                attempt <= 3 ||
                attempt % 10 == 0 ||
                !failure.isTransientMailboxNetworkFailure()
            ) {
                SparrowLog.diagnostic(
                    "ForegroundConnectionHandler",
                    "Mailbox route provisioning retry $attempt deferred",
                    failure
                )
            }

            if (!failure.isTransientMailboxNetworkFailure()) return

            backoffMilliseconds =
                (backoffMilliseconds * 2)
                    .coerceAtMost(
                        MAILBOX_PROVISIONING_MAX_RETRY_MILLISECONDS
                    )
        }
    }

    private fun TransportConnectionState.traceName(): String =
        when (this) {
            is TransportConnectionState.Connected -> "Connected"
            is TransportConnectionState.Connecting -> "Connecting"
            is TransportConnectionState.Disconnected -> "Disconnected"
            is TransportConnectionState.Failed -> "Failed"
        }

    private fun Throwable.isTransientMailboxNetworkFailure(): Boolean {
        if (isRecoverableConnectivityFailure()) return true

        var current: Throwable? = this
        repeat(8) {
            val cause = current ?: return false
            if (
                cause::class.simpleName == "SSLProtocolException" &&
                cause.message?.contains(
                    "TLSV1_ALERT_INTERNAL_ERROR",
                    ignoreCase = true
                ) == true
            ) {
                return true
            }
            current = cause.cause
        }

        return false
    }

    private companion object {
        const val MAILBOX_PROVISIONING_INITIAL_RETRY_MILLISECONDS = 2_000L
        const val MAILBOX_PROVISIONING_MAX_RETRY_MILLISECONDS = 60_000L
    }
}
