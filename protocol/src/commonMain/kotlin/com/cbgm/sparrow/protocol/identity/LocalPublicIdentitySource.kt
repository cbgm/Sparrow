package com.cbgm.sparrow.protocol.identity

interface LocalPublicIdentitySource {
    suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity>
}
