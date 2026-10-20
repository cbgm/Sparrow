package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository

class ClearPaymentContextUseCase(
    private val repository: PaymentComposerRepository
) {
    operator fun invoke(groupId: String): Result<Unit> = repository.clearContext(groupId)
}
