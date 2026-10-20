package com.cbgm.sparrow.protocol.messagepart

import com.cbgm.sparrow.core.base.messagepart.data.model.PaymentTransferDto
import com.cbgm.sparrow.core.base.messagepart.data.model.TextDto
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PaymentWireValidationTest {
    private val event = PaymentTransferDto("transfer-sent", "transfer", "board", "payer", "recipient", 100L, "EUR", "SPARROW-test", "SENT", 1L)

    @Test fun requiresStandaloneGroupMessage() {
        listOf(event).requireValidWireMessageParts(allowExpenses = true)
        assertFailsWith<IllegalArgumentException> { listOf(event).requireValidWireMessageParts() }
        assertFailsWith<IllegalArgumentException> {
            listOf(event, TextDto("text", "extra")).requireValidWireMessageParts(allowExpenses = true)
        }
    }

    @Test fun rejectsUnknownStatusAndInvalidEventIdentity() {
        assertFailsWith<IllegalArgumentException> { listOf(event.copy(status = "SUCCESS")).requireValidWireMessageParts(allowExpenses = true) }
        assertFailsWith<IllegalArgumentException> { listOf(event.copy(id = "other")).requireValidWireMessageParts(allowExpenses = true) }
    }
}
