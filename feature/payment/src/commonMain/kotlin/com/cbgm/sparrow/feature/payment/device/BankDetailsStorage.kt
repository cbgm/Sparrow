package com.cbgm.sparrow.feature.payment.device

/** Implementations protect values with platform key storage and exclude device backups. */
internal interface BankDetailsStorage {
    suspend fun read(key: String): String?

    suspend fun write(key: String, value: String)

    suspend fun delete(key: String)
}
