package com.cbgm.sparrow.feature.transport.domain.rule

import com.cbgm.sparrow.feature.transport.domain.model.FileTransferFailedException
import com.cbgm.sparrow.feature.transport.domain.model.ProtectedRouteMissingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/** True when this failure, or one behind it, is a [FileTransferFailedException]. */
fun Throwable.isFileTransferFailure(): Boolean = causes().any { failure -> failure is FileTransferFailedException }

/** True when this failure, or one behind it, is [ProtectedRouteMissingException]: the user's choice held it back. */
fun Throwable.isWaitingForRoute(): Boolean = causes().any { failure -> failure is ProtectedRouteMissingException }

/**
 * Connectivity failures are handled by the transport's retry/cooldown machinery and
 * by the existing app-wide offline/reconnected hint. They are not application errors.
 *
 * Keep this narrowly scoped: protocol, identity, validation and storage failures are
 * NOT classified as offline, even when they occur while establishing a connection.
 * This code lives in commonMain and checks the platform exception chain by type name
 * because JVM and Darwin socket exceptions do not share a common Kotlin type.
 * The first cancellation or connectivity failure in the chain decides; only a timeout counts among cancellations.
 */
fun Throwable.isRecoverableConnectivityFailure(): Boolean =
    causes()
        .firstOrNull { failure -> failure is CancellationException || failure::class.simpleName in CONNECTIVITY_FAILURES }
        ?.let { failure -> failure !is CancellationException || failure is TimeoutCancellationException }
        ?: false

/** A valid but empty/offline discovery result means the node network is unavailable. */
internal fun Throwable.isUnavailableNodeDirectory(): Boolean =
    this is IllegalStateException &&
        (
            message == "Node directory does not contain a gateway node to probe" ||
                message == "No control plane is configured" ||
                message == "No reachable control plane is configured"
        )

/** This failure and the ones behind it, at most [MAXIMUM_CAUSES] deep. */
private fun Throwable.causes(): Sequence<Throwable> = generateSequence(this) { failure -> failure.cause }.take(MAXIMUM_CAUSES)

private val CONNECTIVITY_FAILURES =
    setOf(
        "ConnectException",
        "ConnectTimeoutException",
        "SocketTimeoutException",
        "UnknownHostException",
        "UnresolvedAddressException",
        "NoRouteToHostException",
        "PortUnreachableException",
        "SocketException",
        "EOFException",
        "HttpRequestTimeoutException"
    )

private const val MAXIMUM_CAUSES = 8
