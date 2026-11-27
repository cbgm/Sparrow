package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSelection
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSession
import com.cbgm.sparrow.feature.payment.domain.model.PaymentUnavailableException
import kotlinx.coroutines.flow.first

class RecordSelectedPaymentUseCase(
    private val observeSession: ObservePaymentSessionUseCase,
    private val recordStatus: RecordPaymentStatusUseCase
) {
    suspend operator fun invoke(selection: PaymentSelection, status: PaymentTransferStatus): Result<Unit> {
        val session = observeSession(selection).first() ?: return Result.failure(PaymentUnavailableException())
        val timestamp = SystemClock.nowEpochMilliseconds()
        val event =
            when (status) {
                PaymentTransferStatus.SENT -> sentTransfer(selection, session, timestamp)
                PaymentTransferStatus.RECEIVED -> receivedTransfer(selection, session, timestamp)
            } ?: return Result.failure(PaymentUnavailableException())
        return recordStatus(selection.groupId, event)
    }

    private fun sentTransfer(
        selection: PaymentSelection,
        session: PaymentSession,
        timestamp: Long
    ): PaymentTransfer? {
        if (!session.canPay) return null
        return PaymentTransfer(
            id = "${selection.transferId}-sent",
            transferId = selection.transferId,
            boardId = selection.boardId,
            fromMemberId = selection.fromMemberId,
            toMemberId = selection.toMemberId,
            amountMinor = selection.amountMinor,
            currencyCode = session.currencyCode,
            reference = session.reference,
            status = PaymentTransferStatus.SENT,
            occurredAtEpochMilliseconds = timestamp
        )
    }

    private fun receivedTransfer(
        selection: PaymentSelection,
        session: PaymentSession,
        timestamp: Long
    ): PaymentTransfer? {
        if (!session.canConfirmReceipt) return null
        return session.sentTransfer?.copy(
            id = "${selection.transferId}-received",
            status = PaymentTransferStatus.RECEIVED,
            occurredAtEpochMilliseconds = timestamp
        )
    }
}
