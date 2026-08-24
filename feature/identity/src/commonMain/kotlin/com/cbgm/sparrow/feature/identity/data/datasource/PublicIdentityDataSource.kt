package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity

interface PublicIdentityDataSource {
    suspend fun save(identity: PublicIdentity): Result<Unit>

    suspend fun load(): Result<PublicIdentity?>

    suspend fun exists(): Result<Boolean>

    suspend fun delete(): Result<Unit>
}
