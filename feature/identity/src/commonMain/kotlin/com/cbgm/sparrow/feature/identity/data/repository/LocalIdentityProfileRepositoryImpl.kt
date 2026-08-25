package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.feature.identity.data.datasource.LocalIdentityProfileDataSource
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import kotlinx.coroutines.flow.Flow

class LocalIdentityProfileRepositoryImpl(
    private val dataSource: LocalIdentityProfileDataSource
) : LocalIdentityProfileRepository {
    override fun observePhoneNumber(): Flow<String?> = dataSource.observePhoneNumber()

    override suspend fun loadPhoneName(): Result<Pair<String, String>?> = dataSource.loadPhoneName()

    override suspend fun loadPhoneNumber(): Result<String?> = dataSource.loadPhoneNumber()

    override suspend fun savePhoneName(
        phoneNumber: String,
        name: String
    ): Result<Unit> =
        runCatching {
            require(phoneNumber.isNotBlank()) { "Phone number must not be blank" }
            dataSource.savePhoneName(phoneNumber = phoneNumber, name = name).getOrThrow()
        }

    override suspend fun deletePhoneName(): Result<Unit> = dataSource.deletePhoneName()
}
