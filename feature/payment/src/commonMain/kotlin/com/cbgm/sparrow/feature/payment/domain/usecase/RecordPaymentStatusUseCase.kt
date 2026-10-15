package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository

class RecordPaymentStatusUseCase(
    private val repository: PaymentComposerRepository
) {
    suspend operator fun invoke(groupId: String, event: PaymentTransfer): Result<Unit> = repository.record(groupId, event)
}
