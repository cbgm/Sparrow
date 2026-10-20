package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.repository.BankDetailsRepository

class FindBankDetailsUseCase(
    private val repository: BankDetailsRepository
) {
    suspend operator fun invoke(key: String): Result<BankDetails?> = repository.find(key)
}
