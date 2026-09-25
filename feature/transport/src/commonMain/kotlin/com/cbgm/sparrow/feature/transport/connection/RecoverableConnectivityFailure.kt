package com.cbgm.sparrow.feature.transport.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Connectivity failures are handled by the transport's retry/cooldown machinery and
 * by the existing app-wide offline/reconnected hint. They are not application errors.
 *
 * Keep this narrowly scoped: protocol, identity, validation and storage failures are
 * NOT classified as offline, even when they occur while establishing a connection.
 * This code lives in commonMain and checks the platform exception chain by type name
 * because JVM and Darwin socket exceptions do not share a common Kotlin type.
 */
fun Throwable.isRecoverableConnectivityFailure(): Boolean {
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth++ < MAX_CAUSE_DEPTH) {
        if (current is TimeoutCancellationException) return true
        if (current is CancellationException) return false
        when (current::class.simpleName) {
            "ConnectException",
            "ConnectTimeoutException",
            "SocketTimeoutException",
            "UnknownHostException",
            "UnresolvedAddressException",
            "NoRouteToHostException",
            "PortUnreachableException",
            "SocketException",
            "EOFException",
            "HttpRequestTimeoutException" -> return true
        }
        current = current.cause
    }
    return false
}

/** A valid but empty/offline discovery result means the node network is unavailable. */
internal fun Throwable.isUnavailableNodeDirectory(): Boolean =
    this is IllegalStateException &&
        (
            message == "Node directory does not contain a gateway node to probe" ||
                message == "No control plane is configured" ||
                message == "No reachable control plane is configured"
        )

private const val MAX_CAUSE_DEPTH = 8
