package com.cbgm.sparrow.feature.membership.domain.model

/** Read-only routing identity for a member whose signing key is installed in a group epoch. */
class GroupTransportRoutingMember(
    val contactId: String,
    val signingPublicKey: ByteArray
)
