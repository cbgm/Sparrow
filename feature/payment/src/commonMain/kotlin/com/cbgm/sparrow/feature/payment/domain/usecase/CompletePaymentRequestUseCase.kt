package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.model.PaymentRequest
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository

class CompletePaymentRequestUseCase(
    private val repository: PaymentComposerRepository
) {
    operator fun invoke(request: PaymentRequest, result: Result<Unit>) = repository.complete(request, result)
}
