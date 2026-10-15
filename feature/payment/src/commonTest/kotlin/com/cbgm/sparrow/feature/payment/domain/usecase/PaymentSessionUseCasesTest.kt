package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.feature.payment.data.repository.PaymentComposerRepositoryImpl
import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.model.InvalidPaymentDetailsException
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.model.PaymentMember
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSelection
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSettlement
import com.cbgm.sparrow.feature.payment.domain.model.PaymentUnavailableException
import com.cbgm.sparrow.feature.payment.domain.repository.BankDetailsRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PaymentSessionUseCasesTest {
    @Test
    fun statusRequiresMatchingReceiptAndConfirmedTransfersCannotBePaidAgain() = runTest {
        val fixture = PaymentFixture()
        fixture.publish()
        assertTrue(fixture.observe(fixture.selection).first()?.canPay == true)
        fixture.publish(fixture.context.copy(transfers = listOf(fixture.sent)))
        val pending = fixture.observe(fixture.selection).first()!!
        assertEquals(PaymentTransferStatus.SENT, pending.status)
        assertFalse(pending.canPay)
        val receipt = fixture.sent.copy(id = "payment-received", status = PaymentTransferStatus.RECEIVED)
        fixture.publish(fixture.context.copy(transfers = listOf(fixture.sent, receipt.copy(amountMinor = 1L))))
        assertEquals(PaymentTransferStatus.SENT, fixture.observe(fixture.selection).first()?.status)
        fixture.publish(fixture.context.copy(transfers = listOf(fixture.sent, receipt)))
        assertEquals(PaymentTransferStatus.RECEIVED, fixture.observe(fixture.selection).first()?.status)
    }

    @Test
    fun preparingAndRecordingRecheckTheCurrentSettlement() = runTest {
        val fixture = PaymentFixture()
        fixture.publish()
        fixture.publish(fixture.context.copy(settlements = listOf(PaymentSettlement("payer", "recipient", 6000L))))
        assertIs<PaymentUnavailableException>(fixture.prepare(fixture.selection, fixture.bank, true).exceptionOrNull())
        assertIs<PaymentUnavailableException>(fixture.record(fixture.selection, PaymentTransferStatus.SENT).exceptionOrNull())
        assertEquals(0, fixture.banks.writes)
    }

    @Test
    fun reviewNormalizesValidDetailsAndHonoursStorageChoice() = runTest {
        val fixture = PaymentFixture()
        fixture.publish()
        val invalid = fixture.prepare(fixture.selection, fixture.bank.copy(iban = "invalid"), true)
        assertIs<InvalidPaymentDetailsException>(invalid.exceptionOrNull())
        assertEquals(0, fixture.banks.writes)
        val prepared = fixture.prepare(fixture.selection, fixture.bank.copy(iban = "de89 3704 0044 0532 0130 00"), true)
        assertTrue(prepared.isSuccess)
        assertEquals(fixture.bank, fixture.banks.details)
        assertTrue(prepared.getOrNull()?.epcPayload?.contains("EUR50.00") == true)
        assertTrue(fixture.prepare(fixture.selection, fixture.bank, false).isSuccess)
        assertEquals(null, fixture.banks.details)
        assertEquals(1, fixture.banks.deletes)
    }

    @Test
    fun payerCannotConfirmReceiptAndHostSendFailureReachesTheCaller() = runTest {
        val fixture = PaymentFixture()
        fixture.publish()
        assertIs<PaymentUnavailableException>(fixture.record(fixture.selection, PaymentTransferStatus.RECEIVED).exceptionOrNull())
        var result: Result<Unit>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            result = fixture.record(fixture.selection, PaymentTransferStatus.SENT)
        }
        val request = fixture.composer.observeRequests("group").first()
        assertEquals(fixture.sent.reference, request.event.reference)
        assertEquals(fixture.selection.amountMinor, request.event.amountMinor)
        val failure = IllegalStateException("send failed")
        fixture.composer.complete(request, Result.failure(failure))
        job.join()
        assertSame(failure, result?.exceptionOrNull())
    }

    @Test
    fun recipientConfirmationPreservesOriginalTransferFields() = runTest {
        val fixture = PaymentFixture()
        fixture.publish(fixture.context.copy(localMemberId = "recipient", transfers = listOf(fixture.sent)))
        var result: Result<Unit>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            result = fixture.record(fixture.selection, PaymentTransferStatus.RECEIVED)
        }
        val request = fixture.composer.observeRequests("group").first()
        assertEquals(
            fixture.sent.copy(
                id = "payment-received",
                status = PaymentTransferStatus.RECEIVED,
                occurredAtEpochMilliseconds = request.event.occurredAtEpochMilliseconds
            ),
            request.event
        )
        fixture.composer.complete(request, Result.success(Unit))
        job.join()
        assertTrue(result?.isSuccess == true)
    }
}

internal class PaymentFixture {
    val composer = PaymentComposerRepositoryImpl()
    val banks = TestBankDetailsRepository()
    val bank = BankDetails("Jörg Müller", "DE89370400440532013000")
    val selection = PaymentSelection("group", "board", "payer", "recipient", 5000L, "payment")
    val context = PaymentContext(
        "board",
        "EUR",
        "payer",
        listOf(PaymentMember("payer", "Payer"), PaymentMember("recipient", "Recipient")),
        listOf(PaymentSettlement("payer", "recipient", 5000L)),
        emptyList()
    )
    val sent = PaymentTransfer(
        "payment-sent",
        "payment",
        "board",
        "payer",
        "recipient",
        5000L,
        "EUR",
        "SPARROW-payment",
        PaymentTransferStatus.SENT,
        1L
    )
    val observe = ObservePaymentSessionUseCase(ObservePaymentContextUseCase(composer))
    val prepare = PreparePaymentUseCase(observe, SaveBankDetailsUseCase(banks), DeleteBankDetailsUseCase(banks))
    val record = RecordSelectedPaymentUseCase(observe, RecordPaymentStatusUseCase(composer))

    fun publish(value: PaymentContext = context) {
        composer.publishContext("group", value).getOrThrow()
    }
}

internal class TestBankDetailsRepository : BankDetailsRepository {
    var details: BankDetails? = null
    var writes = 0
    var deletes = 0

    override suspend fun find(key: String): Result<BankDetails?> = Result.success(details)

    override suspend fun save(key: String, details: BankDetails): Result<Unit> {
        this.details = details
        writes++
        return Result.success(Unit)
    }

    override suspend fun delete(key: String): Result<Unit> {
        details = null
        deletes++
        return Result.success(Unit)
    }
}
