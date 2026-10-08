package com.cbgm.sparrow.protocol.identity

interface LocalPublicIdentityProvider {
    suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity>
}
