package com.cbgm.sparrow.core.protocol.identity

interface LocalIdentityChangeHandler {
    suspend fun onLocalIdentityChanged(): Result<Unit>
}
