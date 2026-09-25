package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagingIndicatorUseCasesTest {
    @Test
    fun genericIndicatorPreservesSenderTypeAndRoutingDestination() = runTest {
        val gateway = FakeGateway()
        val incoming = ObserveMessagingIndicatorsUseCase(gateway)().first()
        assertEquals("peer-routing", incoming.senderRoutingId)
        assertEquals("TYPING", incoming.indicatorType)
        val sent = SendMessagingIndicatorUseCase(gateway)("recipient-routing", "RECORDING")
        assertTrue(sent.isSuccess)
        assertEquals("recipient-routing" to "RECORDING", gateway.lastSent)
    }

    private class FakeGateway : MessagingIndicatorGateway {
        override val incoming: Flow<MessagingIndicator> = flowOf(MessagingIndicator("peer-routing", "TYPING"))
        var lastSent: Pair<String, String>? = null

        override suspend fun send(recipientRoutingId: String, indicatorType: String): Result<Unit> {
            lastSent = recipientRoutingId to indicatorType
            return Result.success(Unit)
        }
    }
}
