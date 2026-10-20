package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferPolicy
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import kotlinx.coroutines.flow.first

class RequireGroupPaymentTransferUseCase(
    private val findMembers: FindGroupExpenseMembersUseCase,
    private val attachments: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(groupId: String, event: PaymentTransfer): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val members = findMembers(groupId).getOrThrow()
            require(event.boardId == members.board.id && event.currencyCode == members.board.currencyCode)
            require(event.fromMemberId in members.memberIds && event.toMemberId in members.memberIds)
            PaymentTransferPolicy.requireActor(event, members.localMemberId)
            val stored = attachments.observePaymentTransfers(groupId).first()
                .filter { it.boardId == event.boardId && it.transferId == event.transferId }
            require(stored.none { it.status == event.status }) { "Payment status already recorded" }
            if (event.status == PaymentTransferStatus.RECEIVED) {
                require(stored.any { it.status == PaymentTransferStatus.SENT && PaymentTransferPolicy.matches(it, event) }) {
                    "No matching sent payment exists"
                }
            }
        }
}
