package com.cbgm.sparrow.feature.payment.domain.repository

import com.cbgm.sparrow.feature.payment.domain.model.BankDetails

interface BankDetailsRepository {
    suspend fun find(key: String): Result<BankDetails?>

    suspend fun save(key: String, details: BankDetails): Result<Unit>

    suspend fun delete(key: String): Result<Unit>
}
