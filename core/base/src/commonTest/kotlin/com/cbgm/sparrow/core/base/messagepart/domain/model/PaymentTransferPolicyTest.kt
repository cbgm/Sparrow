package com.cbgm.sparrow.core.base.messagepart.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentTransferPolicyTest {
    private val sent = PaymentTransfer("transfer-sent", "transfer", "board", "payer", "recipient", 1000L, "EUR", "SPARROW-test", PaymentTransferStatus.SENT, 1L)
    private val receipt = sent.copy(id = "transfer-received", status = PaymentTransferStatus.RECEIVED)

    @Test fun onlyTheAuthenticatedParticipantCanReportTheirAction() {
        PaymentTransferPolicy.requireActor(sent, "payer")
        PaymentTransferPolicy.requireActor(receipt, "recipient")
        assertFailsWith<IllegalArgumentException> { PaymentTransferPolicy.requireActor(sent, "recipient") }
        assertFailsWith<IllegalArgumentException> { PaymentTransferPolicy.requireActor(receipt, "payer") }
        assertFailsWith<IllegalArgumentException> { PaymentTransferPolicy.requireActor(receipt, "admin") }
    }

    @Test fun confirmationMustMatchTheOriginalTransferExactly() {
        assertTrue(PaymentTransferPolicy.matches(sent, receipt))
        assertFalse(PaymentTransferPolicy.matches(sent, receipt.copy(amountMinor = 999L)))
        assertFalse(PaymentTransferPolicy.matches(sent, receipt.copy(boardId = "other")))
        assertFalse(PaymentTransferPolicy.matches(sent, receipt.copy(toMemberId = "other")))
        assertFalse(PaymentTransferPolicy.matches(sent, receipt.copy(reference = "SPARROW-other")))
    }

    @Test fun oneImmutableIdPerStatusPreventsConflictingRevisions() {
        assertFailsWith<IllegalArgumentException> { PaymentTransferPolicy.requireValid(sent.copy(id = "new-id")) }
        assertFailsWith<IllegalArgumentException> { PaymentTransferPolicy.requireValid(sent.copy(currencyCode = "USD")) }
        assertFailsWith<IllegalArgumentException> { PaymentTransferPolicy.requireValid(sent.copy(amountMinor = 0L)) }
    }
}
