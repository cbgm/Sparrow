package com.cbgm.sparrow.core.crypto.identity

interface IdentityKeyGenerator {
    /**
     * Generates:
     *
     * - an X25519/Curve25519 encryption key pair;
     * - an Ed25519 signing key pair.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun generate(): Result<IdentityKeyPair>
}
