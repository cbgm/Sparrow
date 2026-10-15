package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository

class PublishPaymentContextUseCase(
    private val repository: PaymentComposerRepository
) {
    operator fun invoke(groupId: String, context: PaymentContext?): Result<Unit> = repository.publishContext(groupId, context)
}
