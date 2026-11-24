package com.cbgm.sparrow.protocol.identity

/** The user's choice how a new direct contact gets this device's identity. */
interface DirectIdentitySetupModeSource {
    /** True when the identity goes out with the invitation itself, false when the user shares it by hand. */
    suspend fun sharesIdentityAutomatically(): Boolean
}
