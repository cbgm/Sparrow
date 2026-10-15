package com.cbgm.sparrow.feature.expenses.domain

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus

internal object ExpensePayments {
    fun confirmed(board: ExpenseBoard, transfers: List<PaymentTransfer>): List<PaymentTransfer> =
        transfers.filter { it.boardId == board.id }.groupBy(PaymentTransfer::transferId).values.mapNotNull { events ->
            events.forEach(PaymentTransferPolicy::requireValid)
            val sent = events.filter { it.status == PaymentTransferStatus.SENT }
                .minWithOrNull(compareBy(PaymentTransfer::occurredAtEpochMilliseconds, PaymentTransfer::id))
            sent?.takeIf { payment ->
                payment.currencyCode == board.currencyCode && events.any {
                    it.status == PaymentTransferStatus.RECEIVED && PaymentTransferPolicy.matches(payment, it)
                }
            }
        }
}
