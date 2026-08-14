package com.cbgm.sparrow.core.protocol.outbox

data class OutboxProcessingResult(
    val processedCount: Int,
    val sentCount: Int,
    val failedCount: Int
) {
    init {
        require(processedCount >= 0)
        require(sentCount >= 0)
        require(failedCount >= 0)

        require(sentCount + failedCount == processedCount) {
            "Sent and failed counts must equal processed count"
        }
    }
}

interface OutboxProcessor {
    /**
     * Processes up to [limit] pending packets.
     *
     * The implementation must continue processing remaining items when
     * one item fails.
     */
    suspend fun processPending(limit: Int = 20): Result<OutboxProcessingResult>
}
