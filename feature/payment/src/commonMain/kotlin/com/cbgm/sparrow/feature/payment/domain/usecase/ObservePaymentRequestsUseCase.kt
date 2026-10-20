package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.model.PaymentRequest
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository
import kotlinx.coroutines.flow.Flow

class ObservePaymentRequestsUseCase(
    private val repository: PaymentComposerRepository
) {
    operator fun invoke(groupId: String): Flow<PaymentRequest> = repository.observeRequests(groupId)
}
