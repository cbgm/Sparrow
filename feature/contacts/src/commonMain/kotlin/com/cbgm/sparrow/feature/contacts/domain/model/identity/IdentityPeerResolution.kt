package com.cbgm.sparrow.feature.contacts.domain.model.identity

data class IdentityPeerResolution(
    val peerId: String,
    val wasKnownPeer: Boolean,
    val merges: List<IdentityPeerMerge> = emptyList(),
    /** An unused phone book contact with the claimed number; linked only when the user accepts. */
    val phoneContactPeerId: String? = null
)

data class IdentityPeerMerge(
    val fromPeerId: String,
    val toPeerId: String,
    val moveBootstrapRouting: Boolean
)
