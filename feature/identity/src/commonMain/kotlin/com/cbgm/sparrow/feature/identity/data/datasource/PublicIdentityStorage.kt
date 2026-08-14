package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity

interface PublicIdentityStorage {
    suspend fun save(identity: PublicIdentity): Result<Unit>

    suspend fun load(): Result<PublicIdentity?>

    suspend fun exists(): Result<Boolean>

    /**
     * Deletes the locally stored public identity.
     *
     * Used for rollback and later for explicit identity reset.
     */
    suspend fun delete(): Result<Unit>
}
