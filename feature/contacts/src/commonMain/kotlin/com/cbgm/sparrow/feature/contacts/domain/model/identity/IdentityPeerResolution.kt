package com.cbgm.sparrow.feature.contacts.domain.model.identity

data class IdentityPeerResolution(
    val peerId: String,
    val wasKnownPeer: Boolean,
    val merges: List<IdentityPeerMerge> = emptyList()
)

data class IdentityPeerMerge(
    val fromPeerId: String,
    val toPeerId: String,
    val moveBootstrapRouting: Boolean
)
