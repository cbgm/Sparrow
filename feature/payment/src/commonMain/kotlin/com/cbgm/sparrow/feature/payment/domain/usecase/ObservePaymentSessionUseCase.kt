package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSelection
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePaymentSessionUseCase(
    private val observeContext: ObservePaymentContextUseCase
) {
    operator fun invoke(selection: PaymentSelection): Flow<PaymentSession?> =
        observeContext(selection.groupId).map { context ->
            context?.takeIf { it.boardId == selection.boardId }?.resolve(selection)
        }

    private fun PaymentContext.resolve(selection: PaymentSelection): PaymentSession {
        val sent = transfers.filter {
            it.boardId == selection.boardId && it.transferId == selection.transferId &&
                it.status == PaymentTransferStatus.SENT && it.fromMemberId == selection.fromMemberId &&
                it.toMemberId == selection.toMemberId && it.amountMinor == selection.amountMinor &&
                it.currencyCode == currencyCode
        }.minWithOrNull(compareBy(PaymentTransfer::occurredAtEpochMilliseconds, PaymentTransfer::id))
        val received = sent != null && transfers.any {
            it.status == PaymentTransferStatus.RECEIVED && PaymentTransferPolicy.matches(sent, it)
        }
        val isPayer = localMemberId == selection.fromMemberId
        return PaymentSession(
            selection = selection,
            bankDetailsKey = "$localMemberId:${selection.toMemberId}",
            payerName = members.firstOrNull { it.id == selection.fromMemberId }?.displayName.orEmpty(),
            recipientName = members.firstOrNull { it.id == selection.toMemberId }?.displayName.orEmpty(),
            currencyCode = currencyCode,
            reference = sent?.reference ?: reference(selection.transferId),
            isPayer = isPayer,
            isRecipient = localMemberId == selection.toMemberId,
            canPay = currencyCode == "EUR" && isPayer && sent == null && settlements.any {
                it.fromMemberId == selection.fromMemberId && it.toMemberId == selection.toMemberId &&
                    it.amountMinor == selection.amountMinor
            },
            sentTransfer = sent,
            status = when {
                received -> PaymentTransferStatus.RECEIVED
                sent != null -> PaymentTransferStatus.SENT
                else -> null
            }
        )
    }

    private fun reference(transferId: String): String =
        "SPARROW-" + transferId.filter { it.isLetterOrDigit() || it == '-' }.takeLast(40)
}
