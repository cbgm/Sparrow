package com.cbgm.sparrow.data.database.identity

import com.cbgm.sparrow.data.database.SparrowDatabase

interface LocalIdentityDataResetter {
    suspend fun reset()
}

internal class RoomLocalIdentityDataResetter(
    private val database: SparrowDatabase
) : LocalIdentityDataResetter {
    override suspend fun reset() {
        database.clearAllTables()
    }
}
