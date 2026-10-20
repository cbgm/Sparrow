package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.repository.BankDetailsRepository

class SaveBankDetailsUseCase(
    private val repository: BankDetailsRepository
) {
    suspend operator fun invoke(key: String, details: BankDetails): Result<Unit> = repository.save(key, details)
}
