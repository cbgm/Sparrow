package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.feature.transport.mailbox.MailboxCreationWork
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable

/**
 * This device's invitation mailbox and its encryption key (same wire format as `InvitationRoute` in
 * `:server:protocol`). It goes into the alias claims; senders that have no mailbox route of this device yet put their
 * packets there, sealed for [encryptionPublicKey].
 */
@Serializable
internal class InvitationRoute(
    val route: MailboxDeliveryRoute,
    val encryptionPublicKey: ByteArray
)

/** `POST /v1/invitation-routes`: [routingId] is a phone alias or a device routing ID. */
@Serializable
internal class InvitationRouteLookup(
    val routingId: String,
    val work: MailboxCreationWork
)

/** The device ([ownerRoutingId]; for a phone alias the device holding it) and its [invitationRoute]. */
@Serializable
internal class InvitationRouteResult(
    val ownerRoutingId: String,
    val invitationRoute: InvitationRoute
)
