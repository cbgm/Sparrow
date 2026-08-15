package com.cbgm.sparrow.server.gateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

internal class GatewaySessionWorkDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queues = ConcurrentHashMap<String, Channel<suspend () -> Unit>>()
    private val independentSlots = Semaphore(MAX_CONCURRENT_INDEPENDENT_WORK)

    fun dispatch(
        key: String,
        work: suspend () -> Unit
    ) {
        require(key.isNotBlank()) {
            "Gateway work key must not be blank"
        }

        queueFor(key).trySend(work)
    }

    fun dispatchIndependent(work: suspend () -> Unit) {
        scope.launch {
            independentSlots.withPermit {
                runCatching {
                    work()
                }
            }
        }
    }

    fun close() {
        scope.cancel()
        queues.clear()
    }

    private fun queueFor(key: String): Channel<suspend () -> Unit> =
        queues.computeIfAbsent(key) {
            Channel<suspend () -> Unit>(capacity = Channel.UNLIMITED).also { queue ->
                scope.launch {
                    for (work in queue) {
                        runCatching {
                            work()
                        }
                    }
                }
            }
        }

    private companion object {
        const val MAX_CONCURRENT_INDEPENDENT_WORK = 32
    }
}
