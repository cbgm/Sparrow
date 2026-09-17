package com.cbgm.sparrow.feature.conversationorchestration.domain.port

interface OrchestratedOutboxDeliveryPort {
    fun canHandle(packetId: String): Boolean

    suspend fun onFailed(packetId: String)
}
