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
    fun genericIndicatorPreservesSenderTypeAndDestination() = runTest {
        val gateway = FakeGateway()
        val incoming = ObserveMessagingIndicatorsUseCase(gateway)().first()
        assertEquals("peer", incoming.senderContactId)
        assertEquals("TYPING", incoming.indicatorType)
        val sent = SendMessagingIndicatorUseCase(gateway)("recipient", "group-1", "RECORDING")
        assertTrue(sent.isSuccess)
        assertEquals(Triple("recipient", "group-1", "RECORDING"), gateway.lastSent)
    }

    private class FakeGateway : MessagingIndicatorGateway {
        override val incoming: Flow<MessagingIndicator> = flowOf(MessagingIndicator("peer", null, "TYPING"))
        var lastSent: Triple<String, String?, String>? = null

        override suspend fun send(
            recipientContactId: String,
            groupId: String?,
            indicatorType: String
        ): Result<Unit> {
            lastSent = Triple(recipientContactId, groupId, indicatorType)
            return Result.success(Unit)
        }
    }
}
