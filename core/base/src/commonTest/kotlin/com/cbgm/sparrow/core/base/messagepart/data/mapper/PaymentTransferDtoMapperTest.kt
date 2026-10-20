package com.cbgm.sparrow.core.base.messagepart.data.mapper

import com.cbgm.sparrow.core.base.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentTransferDtoMapperTest {
    @Test fun roundTripsThroughPolymorphicWireFormatWithoutBankDetails() {
        val event = PaymentTransfer("transfer-sent", "transfer", "board", "payer", "recipient", 1000L, "EUR", "SPARROW-test", PaymentTransferStatus.SENT, 1L)
        val encoded = Json.encodeToString<MessagePartDto>(event.toDto())
        assertTrue(encoded.contains("PAYMENT_TRANSFER"))
        assertFalse(encoded.contains("iban", ignoreCase = true))
        assertFalse(encoded.contains("accountHolder"))
        assertEquals(event, Json.decodeFromString<MessagePartDto>(encoded).toMessagePart())
    }
}
