package com.cbgm.sparrow.feature.transport.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Keeps the app's connection (circuits, mailbox waits, outbox) running while the app is not on screen: the
 * foreground service holds it as long as it runs, a check now and then for a short while. Each [hold] needs a
 * [release].
 */
interface ConnectionKeepAlivePort {
    fun observeHeld(): Flow<Boolean>

    fun hold()

    fun release()
}
