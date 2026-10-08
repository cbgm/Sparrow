package com.cbgm.sparrow.protocol.identity

interface LocalIdentityChangeHandler {
    suspend fun onLocalIdentityChanged(): Result<Unit>
}
