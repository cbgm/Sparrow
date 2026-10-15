package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.repository.BankDetailsRepository

class DeleteBankDetailsUseCase(
    private val repository: BankDetailsRepository
) {
    suspend operator fun invoke(key: String): Result<Unit> = repository.delete(key)
}
