package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.domain.port.MessagingIndicatorPort
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
        val indicatorPort = FakeIndicatorPort()
        val incoming = ObserveMessagingIndicatorsUseCase(indicatorPort)().first()
        assertEquals("peer", incoming.senderContactId)
        assertEquals("TYPING", incoming.indicatorType)
        val sent = SendMessagingIndicatorUseCase(indicatorPort)("recipient", "group-1", "RECORDING")
        assertTrue(sent.isSuccess)
        assertEquals(Triple("recipient", "group-1", "RECORDING"), indicatorPort.lastSent)
    }

    private class FakeIndicatorPort : MessagingIndicatorPort {
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
