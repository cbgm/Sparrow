package com.cbgm.sparrow.data.database.identity

import com.cbgm.sparrow.data.database.SparrowDatabase

interface LocalIdentityDataResetter {
    suspend fun reset()

    /** True when there is at least one chat, i.e. a reset would lose something. */
    suspend fun hasConversations(): Boolean
}

internal class RoomLocalIdentityDataResetter(
    private val database: SparrowDatabase
) : LocalIdentityDataResetter {
    override suspend fun reset() {
        database.clearAllTables()
    }

    override suspend fun hasConversations(): Boolean = database.chatDao().hasConversations()
}
