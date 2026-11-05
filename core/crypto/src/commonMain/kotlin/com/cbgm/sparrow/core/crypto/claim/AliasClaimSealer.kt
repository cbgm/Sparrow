package com.cbgm.sparrow.core.crypto.claim

/**
 * Encrypts the device's alias claim (its phone aliases and phone verification token) for the presence directory, so
 * the gateway that forwards it cannot read it. The server opens it with `AliasClaimKeys`.
 */
interface AliasClaimSealer {
    suspend fun seal(
        claim: ByteArray,
        presencePublicKey: ByteArray
    ): Result<ByteArray>
}
