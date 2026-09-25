package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity

interface PublicIdentityDataSource {
    suspend fun save(identity: PublicIdentity)

    suspend fun load(): PublicIdentity?

    suspend fun exists(): Boolean

    suspend fun delete()
}
