package com.cbgm.sparrow.feature.transport.background

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Keeps the app's connection (circuits, mailbox waits, outbox) running while the app is not on screen: the
 * foreground service holds it as long as it runs, a check now and then for a short while. Each [hold] needs a
 * [release].
 */
class ConnectionKeepAlive {
    private val holders = MutableStateFlow(0)

    fun observeHeld(): Flow<Boolean> = holders.map { count -> count > 0 }.distinctUntilChanged()

    fun hold() {
        holders.update { count -> count + 1 }
    }

    fun release() {
        holders.update { count -> (count - 1).coerceAtLeast(0) }
    }
}
