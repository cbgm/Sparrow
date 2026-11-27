package com.cbgm.sparrow.protocol.identity

interface LocalIdentityChangeHandler {
    suspend fun onLocalIdentityChanged(): Result<Unit>

    /** True when resetting the local identity would delete chats. */
    suspend fun hasConversations(): Result<Boolean>
}
