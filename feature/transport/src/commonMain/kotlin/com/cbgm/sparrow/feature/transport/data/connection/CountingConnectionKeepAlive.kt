package com.cbgm.sparrow.feature.transport.data.connection

import com.cbgm.sparrow.feature.transport.domain.port.ConnectionKeepAlivePort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Counts the holders in memory; held while there is at least one. */
internal class CountingConnectionKeepAlive : ConnectionKeepAlivePort {
    private val holders = MutableStateFlow(0)

    override fun observeHeld(): Flow<Boolean> = holders.map { count -> count > 0 }.distinctUntilChanged()

    override fun hold() {
        holders.update { count -> count + 1 }
    }

    override fun release() {
        holders.update { count -> (count - 1).coerceAtLeast(0) }
    }
}
