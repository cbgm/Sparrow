package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository
import kotlinx.coroutines.flow.Flow

class ObservePaymentContextUseCase(
    private val repository: PaymentComposerRepository
) {
    operator fun invoke(groupId: String): Flow<PaymentContext?> = repository.observeContext(groupId)
}
