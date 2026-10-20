package com.cbgm.sparrow.feature.payment.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.payment.data.mapper.toBankDetails
import com.cbgm.sparrow.feature.payment.data.mapper.toDto
import com.cbgm.sparrow.feature.payment.data.model.BankDetailsDto
import com.cbgm.sparrow.feature.payment.device.BankDetailsStorage
import com.cbgm.sparrow.feature.payment.domain.SepaPaymentPolicy
import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.repository.BankDetailsRepository
import kotlinx.serialization.json.Json

internal class BankDetailsRepositoryImpl(
    private val storage: BankDetailsStorage
) : BankDetailsRepository {
    override suspend fun find(key: String): Result<BankDetails?> = safeSuspendCall(logFailure = false) {
        storage.read(key)?.let { SepaPaymentPolicy.normalize(Json.decodeFromString<BankDetailsDto>(it).toBankDetails()) }
    }

    override suspend fun save(key: String, details: BankDetails): Result<Unit> = safeSuspendCall(logFailure = false) {
        storage.write(key, Json.encodeToString(SepaPaymentPolicy.normalize(details).toDto()))
    }

    override suspend fun delete(key: String): Result<Unit> = safeSuspendCall(logFailure = false) { storage.delete(key) }
}
