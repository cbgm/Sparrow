package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSelection
import com.cbgm.sparrow.feature.payment.domain.model.PaymentUnavailableException
import kotlinx.coroutines.flow.first

class RecordSelectedPaymentUseCase(
    private val observeSession: ObservePaymentSessionUseCase,
    private val recordStatus: RecordPaymentStatusUseCase
) {
    suspend operator fun invoke(selection: PaymentSelection, status: PaymentTransferStatus): Result<Unit> {
        val session = observeSession(selection).first() ?: return Result.failure(PaymentUnavailableException())
        val timestamp = SystemClock.nowEpochMilliseconds()
        val event = when (status) {
            PaymentTransferStatus.SENT -> {
                if (!session.canPay) return Result.failure(PaymentUnavailableException())
                PaymentTransfer(
                    id = "${selection.transferId}-sent",
                    transferId = selection.transferId,
                    boardId = selection.boardId,
                    fromMemberId = selection.fromMemberId,
                    toMemberId = selection.toMemberId,
                    amountMinor = selection.amountMinor,
                    currencyCode = session.currencyCode,
                    reference = session.reference,
                    status = status,
                    occurredAtEpochMilliseconds = timestamp
                )
            }
            PaymentTransferStatus.RECEIVED -> {
                if (!session.canConfirmReceipt) return Result.failure(PaymentUnavailableException())
                val sent = session.sentTransfer ?: return Result.failure(PaymentUnavailableException())
                sent.copy(
                    id = "${selection.transferId}-received",
                    status = status,
                    occurredAtEpochMilliseconds = timestamp
                )
            }
        }
        return recordStatus(selection.groupId, event)
    }
}
