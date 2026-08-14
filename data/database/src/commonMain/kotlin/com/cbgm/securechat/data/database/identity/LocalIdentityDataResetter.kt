package com.cbgm.securechat.data.database.identity

import com.cbgm.securechat.data.database.SecureChatDatabase

interface LocalIdentityDataResetter {
    suspend fun reset()
}

internal class RoomLocalIdentityDataResetter(
    private val database: SecureChatDatabase
) : LocalIdentityDataResetter {
    override suspend fun reset() {
        database.clearAllTables()
    }
}
