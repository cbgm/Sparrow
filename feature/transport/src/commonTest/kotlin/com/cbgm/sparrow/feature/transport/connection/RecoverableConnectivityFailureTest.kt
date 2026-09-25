package com.cbgm.sparrow.feature.transport.connection

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoverableConnectivityFailureTest {
    @Test
    fun connectionRefusalAndWrappedTimeoutAreOfflineNotApplicationErrors() {
        assertTrue(ConnectException("Connection refused").isRecoverableConnectivityFailure())
        assertTrue(
            IllegalStateException("WebSocket handshake failed", ConnectTimeoutException("Timed out"))
                .isRecoverableConnectivityFailure()
        )
    }

    @Test
    fun protocolAndIdentityErrorsAreNotSuppressed() {
        assertFalse(IllegalStateException("Invalid server signature").isRecoverableConnectivityFailure())
        assertFalse(IllegalArgumentException("Invalid WebSocket URL").isRecoverableConnectivityFailure())
        assertFalse(CancellationException("Stopped").isRecoverableConnectivityFailure())
    }

    @Test
    fun missingNodeIsAnOfflineConditionButOtherIllegalStatesAreNot() {
        assertTrue(
            IllegalStateException("Node directory does not contain a gateway node to probe")
                .isUnavailableNodeDirectory()
        )
        assertFalse(IllegalStateException("Identity not initialized").isUnavailableNodeDirectory())
    }

    private class ConnectException(
        message: String
    ) : Exception(message)

    private class ConnectTimeoutException(
        message: String
    ) : Exception(message)
}
