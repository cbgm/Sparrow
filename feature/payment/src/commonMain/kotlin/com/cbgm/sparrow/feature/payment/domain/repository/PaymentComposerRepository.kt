package com.cbgm.sparrow.feature.payment.domain.repository

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.model.PaymentRequest
import kotlinx.coroutines.flow.Flow

interface PaymentComposerRepository {
    fun observeContext(groupId: String): Flow<PaymentContext?>

    fun publishContext(groupId: String, context: PaymentContext?): Result<Unit>

    fun clearContext(groupId: String): Result<Unit>

    fun observeRequests(groupId: String): Flow<PaymentRequest>

    suspend fun record(groupId: String, event: PaymentTransfer): Result<Unit>

    fun complete(request: PaymentRequest, result: Result<Unit>)
}
