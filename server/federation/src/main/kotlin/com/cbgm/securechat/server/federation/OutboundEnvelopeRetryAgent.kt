package com.cbgm.securechat.server.federation

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory

internal class OutboundEnvelopeRetryAgent(
    private val router: FederationRouter,
    private val pollIntervalMilliseconds: Long,
    private val batchSize: Int
) {
    init {
        require(pollIntervalMilliseconds > 0L)
        require(batchSize > 0)
    }

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            runCatching {
                router.retryPending(batchSize)
            }.onFailure { error ->
                logger.warn("Federation outbound retry pass failed", error)
            }
            delay(pollIntervalMilliseconds)
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(OutboundEnvelopeRetryAgent::class.java)
    }
}
