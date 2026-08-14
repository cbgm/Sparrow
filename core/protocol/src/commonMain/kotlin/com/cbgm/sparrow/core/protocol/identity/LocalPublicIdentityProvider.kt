package com.cbgm.sparrow.core.protocol.identity

interface LocalPublicIdentityProvider {
    suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity>
}
